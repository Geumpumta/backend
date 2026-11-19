import http from 'k6/http';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import exec from 'k6/execution';

const target = Number(__ENV.RANK_RATE);
const duration = 120;
const started = new Counter('rank_started');
const succeeded = new Counter('rank_succeeded');
const errors = new Rate('rank_errors');
const endToEnd = new Trend('rank_e2e', true);
const connect = new Trend('rank_connect', true);
const transfer = new Trend('rank_transfer', true);
const firstStart = new Gauge('rank_measure_start_ms');
const lastEnd = new Gauge('rank_measure_end_ms');

export const options = {
    summaryTrendStats: ['avg', 'p(99)'],
    systemTags: ['scenario'],
    scenarios: {
        warmup: {
            executor: 'constant-arrival-rate', exec: 'warmup', rate: target,
            timeUnit: '1s', duration: '30s', gracefulStop: '5s',
            preAllocatedVUs: target, maxVUs: target * 2,
        },
        measure: {
            executor: 'constant-arrival-rate', exec: 'measure', rate: target,
            timeUnit: '1s', startTime: '35s', duration: `${duration}s`, gracefulStop: '6s',
            preAllocatedVUs: target, maxVUs: target * 2,
        },
    },
    thresholds: {
        rank_e2e: ['p(99)<=200'],
        rank_errors: ['rate<=0.001'],
        rank_started: [`count==${target * duration}`],
        'dropped_iterations{scenario:measure}': ['count==0'],
    },
};

function requestRank(phase) {
    const response = http.get(`${__ENV.RANK_BASE_URL}/api/v1/rank/personal/daily`, {
        headers: {
            Authorization: `Bearer ${(__ENV.RANK_TOKEN || '').trim()}`,
            'X-Rank-Run': __ENV.RANK_RUN_ID,
            'X-Rank-Phase': phase,
        },
        timeout: '5s',
        redirects: 0,
    });
    let ok = false;
    if (response.status === 200) {
        try {
            const body = response.json();
            ok = String(body.success) === 'true' && Array.isArray(body.data.topRanks)
                && Boolean(body.data.myRanking);
        } catch (_) {
            // An invalid response body counts as a failed request.
        }
    }
    return { response, ok };
}

export function setup() {
    const { response, ok } = requestRank('setup');
    if (!ok) {
        let code;
        try {
            const value = response.json('code');
            if (typeof value === 'string' && /^[A-Z][A-Z0-9_]{0,50}$/.test(value)) {
                code = value;
            }
        } catch (_) {
            // The preflight response may be an empty or non-JSON error.
        }
        throw new Error(`Daily rank preflight failed (HTTP ${response.status}${code ? `, code ${code}` : ''}).`);
    }
}

export function warmup() {
    requestRank('warmup');
}

export function measure() {
    started.add(1);
    const start = Date.now();
    firstStart.add(exec.scenario.startTime);
    const { response, ok } = requestRank('measure');
    const end = Date.now();
    lastEnd.add(end);
    endToEnd.add(end - start);
    succeeded.add(ok ? 1 : 0);
    errors.add(!ok);
    connect.add(response.timings.blocked + response.timings.connecting + response.timings.tls_handshaking);
    transfer.add(response.timings.sending + response.timings.receiving);
}

export function handleSummary(data) {
    const metric = (name) => (data.metrics[name] || {}).values || {};
    const issued = metric('rank_started').count || 0;
    const completed = (metric('rank_errors').passes || 0) + (metric('rank_errors').fails || 0);
    const avg = metric('rank_e2e').avg ?? null;
    const p99 = metric('rank_e2e')['p(99)'] ?? null;
    const errorRate = metric('rank_errors').rate ?? null;
    const dropped = metric('dropped_iterations{scenario:measure}').count || 0;
    const report = {
        run: __ENV.RANK_RUN_ID,
        mode: __ENV.RANK_MODE,
        users: Number(__ENV.RANK_USERS),
        targetRps: target,
        durationSeconds: duration,
        issued,
        issuedRps: issued / duration,
        completed,
        successfulRps: (metric('rank_succeeded').count || 0) / duration,
        dropped,
        measurementStartUtc: issued ? new Date(metric('rank_measure_start_ms').value).toISOString() : null,
        dispatchEndUtc: issued ? new Date(metric('rank_measure_start_ms').value + duration * 1000).toISOString() : null,
        lastCompletionUtc: completed ? new Date(metric('rank_measure_end_ms').max).toISOString() : null,
        averageMs: avg,
        p99Ms: p99,
        errorRate,
        verdict: issued !== target * duration || completed !== issued || dropped !== 0
            ? 'incomplete-load'
            : p99 !== null && p99 <= 200 && errorRate !== null && errorRate <= 0.001 ? 'pass' : 'fail',
    };
    const summary = JSON.stringify(report, null, 2);
    return { stdout: summary + '\n', [__ENV.RANK_REPORT]: summary + '\n' };
}
