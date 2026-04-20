package com.gpt.geumpumtabackend.rankbenchmark.controller;

import com.gpt.geumpumtabackend.rankbenchmark.dto.BenchmarkReport;
import com.gpt.geumpumtabackend.rankbenchmark.dto.BenchmarkRunRequest;
import com.gpt.geumpumtabackend.rankbenchmark.generator.StudySessionDataGenerator;
import com.gpt.geumpumtabackend.rankbenchmark.redis.RedisRankingBackfillService;
import com.gpt.geumpumtabackend.rankbenchmark.service.RankingBenchmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rank-benchmark")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class RankingBenchmarkController {

    private final StudySessionDataGenerator generator;
    private final RedisRankingBackfillService backfillService;
    private final RankingBenchmarkService benchmarkService;

    @PostMapping("/generate")
    public ResponseEntity<StudySessionDataGenerator.GenerateReport> generate(
            @RequestParam(required = false) Long totalSessions,
            @RequestParam(required = false) Integer userCount,
            @RequestParam(required = false) Integer startedSessions) {
        return ResponseEntity.ok(generator.generate(totalSessions, userCount, startedSessions));
    }

    @PostMapping("/backfill")
    public ResponseEntity<RedisRankingBackfillService.BackfillReport> backfill() {
        return ResponseEntity.ok(backfillService.backfillAll());
    }

    @PostMapping("/run")
    public ResponseEntity<BenchmarkReport> run(@RequestBody(required = false) BenchmarkRunRequest req) {
        return ResponseEntity.ok(benchmarkService.run(req));
    }
}
