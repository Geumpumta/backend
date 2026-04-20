package com.gpt.geumpumtabackend.rankbenchmark.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record LatencyStats(
        String engine,
        int samples,
        double avgMs,
        double minMs,
        double maxMs,
        double p50Ms,
        double p95Ms,
        double p99Ms
) {
    public static LatencyStats from(String engine, List<Double> timings) {
        if (timings.isEmpty()) {
            return new LatencyStats(engine, 0, 0, 0, 0, 0, 0, 0);
        }
        List<Double> sorted = new ArrayList<>(timings);
        Collections.sort(sorted);
        double sum = 0;
        for (double v : sorted) sum += v;
        double avg = sum / sorted.size();
        return new LatencyStats(
                engine,
                sorted.size(),
                avg,
                sorted.get(0),
                sorted.get(sorted.size() - 1),
                percentile(sorted, 50),
                percentile(sorted, 95),
                percentile(sorted, 99));
    }

    private static double percentile(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
