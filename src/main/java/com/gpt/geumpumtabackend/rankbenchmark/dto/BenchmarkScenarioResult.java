package com.gpt.geumpumtabackend.rankbenchmark.dto;

public record BenchmarkScenarioResult(
        String scenario,
        LatencyStats mysql,
        LatencyStats redis,
        double mysqlOverRedisRatio,
        String winner
) {}
