package com.gpt.geumpumtabackend.rankbenchmark.dto;

public record BenchmarkRunRequest(
        Integer warmup,
        Integer iterations,
        Long userId
) {}
