// node load.cjs <origin> <token-file> <rps> <seconds> <new-output-dir> [probe-path]
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const https = require('node:https');
const { Worker, isMainThread, workerData, parentPort } = require('node:worker_threads');
const { setTimeout: wait } = require('node:timers/promises');

function percentile(values, fraction) {
    return values.slice().sort((a,b) => a-b)[Math.ceil(values.length * fraction)-1] ?? null;
}

async function runWorker() {
    const { origin, tokensFile, rps, seconds, output, probe, worker, runId, preconnect } = workerData;
    const tokens = probe ? [] : fs.readFileSync(tokensFile,'utf8').trim().split(/\r?\n/).filter(Boolean);
    if (!probe && tokens.length < Math.min(rps * 10, 10000)) throw Error('Insufficient distinct fixture tokens for the requested screen population');
    const url = new URL(probe || '/api/v1/rank/personal/daily', origin);
    const transport = url.protocol === 'https:' ? https : http;
    const agent = new transport.Agent({keepAlive:true, maxSockets:512, maxFreeSockets:512, scheduling:'fifo'});
    const pending = new Set();
    const rows = [];
    const clock = () => performance.timeOrigin + performance.now();
    function request(index, planned) {
        const issued = clock();
        const headers = {'X-Rank-Run': runId};
        if (!probe) headers.Authorization = 'Bearer ' + tokens[index % Math.min(rps*10,tokens.length)];
        return new Promise(resolve => {
            let sent = null;
            let finished = false;
            function end(status, error) {
                if (finished) return;
                finished = true;
                clearTimeout(timer);
                const ended = clock();
                rows.push({index,planned,issued,sent,ended,status,error,ms:ended-issued});
                resolve();
            }
            const req = transport.request(url,{agent,headers}, res => {
                const chunks = [];
                res.on('data', c => chunks.push(c));
                res.on('error', e => end(res.statusCode,e.code || 'response'));
                res.on('end', () => {
                    let error = null;
                    if (res.statusCode !== 200) error = 'http';
                    else if (!probe) {
                        try {
                            const body = JSON.parse(Buffer.concat(chunks));
                            if (!Array.isArray(body.data?.topRanks) || !body.data.myRanking) error = 'contract';
                        } catch { error = 'json'; }
                    }
                    end(res.statusCode,error);
                });
            });
            const timer = setTimeout(() => req.destroy(Error('deadline')),5000);
            req.on('finish', () => { sent = clock(); });
            req.on('error', e => end(0,e.code || e.message));
            req.end();
        });
    }
    if (preconnect) {
        const count=Math.min(512, Math.ceil(rps*3/4));
        const attempts = await Promise.allSettled(Array.from({length:count},()=>new Promise((resolve,reject)=>{
            const req=transport.get(new URL('/actuator/health/liveness',origin),{agent},res=>{
                res.resume();
                res.on('error',reject);
                res.on('end',()=>res.statusCode===200 ? resolve() : reject(Error('Socket warm-up HTTP '+res.statusCode)));
            });
            req.setTimeout(10000,()=>req.destroy(Error('Socket warm-up timeout')));
            req.on('error',reject);
        })));
        fs.writeFileSync(path.join(output,'connections-'+worker+'.json'),JSON.stringify({
            attempted:count,ready:attempts.filter(attempt=>attempt.status==='fulfilled').length
        }));
    }
    parentPort.postMessage('ready');
    const start=await new Promise(resolve=>parentPort.once('message',resolve));
    for (let i = worker; i < rps*seconds; i += 4) {
        const planned = start + i*1000/rps;
        const delay = planned-clock();
        if (delay > 0) await wait(delay);
        const task = request(i,planned);
        pending.add(task);
        task.finally(() => pending.delete(task));
    }
    await Promise.all(pending);
    agent.destroy();
    fs.writeFileSync(path.join(output,'worker-'+worker+'.jsonl'),rows.map(r => JSON.stringify(r)).join('\n')+'\n');
    parentPort.postMessage('done');
}

async function main() {
    const [origin,tokensFile,rate,duration,output,probe] = process.argv.slice(2);
    const rps = Number(rate), seconds = Number(duration);
    if (!output || !Number.isSafeInteger(rps) || rps < 1 || !Number.isSafeInteger(seconds) || seconds < 1) throw Error('Usage: node load.cjs <origin> <token-file> <rps> <seconds> <new-output-dir> [probe-path]');
    new URL(origin);
    fs.mkdirSync(output);
    let start;
    const runId = 'rank-'+Date.now();
    const preconnect=process.env.RANK_READ_PRECONNECT==='1';
    const workers = Array.from({length:4},(_,worker) => new Worker(__filename,{
        workerData:{origin,tokensFile,rps,seconds,output,probe,worker,runId,preconnect}
    }));
    let ready=0;
    try { await Promise.all(workers.map(w => new Promise((resolve,reject) => {
        w.on('message',msg=>{
            if (msg==='ready' && ++ready===workers.length) {
                start=Date.now()+500;
                workers.forEach(worker=>worker.postMessage(start));
            }
        });
        w.on('error',reject);
        w.on('exit',code => code ? reject(Error('worker exit '+code)) : resolve());
    }))); } finally {
        await Promise.all(workers.map(w=>w.terminate()));
    }
    const rows = workers.flatMap((_,i) => fs.readFileSync(path.join(output,'worker-'+i+'.jsonl'),'utf8').trim().split('\n').filter(Boolean).map(JSON.parse));
    const ok = rows.filter(r => !r.error);
    const meanMs = ok.length ? ok.reduce((total,row)=>total+row.ms,0)/ok.length : null;
    const p95 = percentile(ok.map(r=>r.ms),.95);
    const p99 = percentile(ok.map(r=>r.ms),.99);
    const dispatchP99 = percentile(rows.map(r=>(r.sent ?? r.ended)-r.planned),.99);
    const summary = {runId,start,rps,seconds,preconnect,planned:rps*seconds,issued:rows.length,success:ok.length,
        successRps:ok.length/seconds,errorRate:1-ok.length/(rps*seconds),meanMs,p95,p99,dispatchP99,
        validGenerator:rows.length===rps*seconds && dispatchP99<=50};
    const bins = Array.from({length:seconds+7},(_,second)=>({second,planned:0,sent:0,success:0,failed:0}));
    const bucket = t => bins[Math.max(0,Math.min(bins.length-1,Math.floor((t-start)/1000)))];
    for (const row of rows) {
        bucket(row.planned).planned++;
        if (row.sent !== null) bucket(row.sent).sent++;
        bucket(row.ended)[row.error ? 'failed' : 'success']++;
    }
    fs.writeFileSync(path.join(output,'seconds.csv'),['second,planned,sent,success,failed',
        ...bins.map(b=>[b.second,b.planned,b.sent,b.success,b.failed].join(','))].join('\n')+'\n');
    fs.writeFileSync(path.join(output,'summary.json'),JSON.stringify(summary,null,2));
    console.log(JSON.stringify(summary));
}

(isMainThread ? main() : runWorker()).catch(e=>{console.error(e.message);process.exitCode=1;});
