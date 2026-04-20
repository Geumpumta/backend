package com.gpt.geumpumtabackend.rankbenchmark.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BenchmarkReport(
        String status,
        int warmupIterations,
        int measureIterations,
        Long userIdForProbing,
        List<BenchmarkScenarioResult> scenarios,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {}
