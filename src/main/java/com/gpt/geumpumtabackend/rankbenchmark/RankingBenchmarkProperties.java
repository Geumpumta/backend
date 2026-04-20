package com.gpt.geumpumtabackend.rankbenchmark;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "benchmark.rank")
@Getter
@Setter
public class RankingBenchmarkProperties {

    private boolean enabled = false;

    private Dataset dataset = new Dataset();

    private Harness harness = new Harness();

    @Getter
    @Setter
    public static class Dataset {
        private long totalSessions = 10_000_000L;
        private int userCount = 20_000;
        private int startedSessionCount = 200;
        private int batchChunk = 5_000;
    }

    @Getter
    @Setter
    public static class Harness {
        private int warmupIterations = 20;
        private int measureIterations = 200;
        private String baseUrl = "http://localhost:8080";
    }
}
