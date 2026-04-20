package com.gpt.geumpumtabackend.rankbenchmark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rankbenchmark.RankingBenchmarkProperties;
import com.gpt.geumpumtabackend.rankbenchmark.dto.BenchmarkReport;
import com.gpt.geumpumtabackend.rankbenchmark.dto.BenchmarkRunRequest;
import com.gpt.geumpumtabackend.rankbenchmark.dto.BenchmarkScenarioResult;
import com.gpt.geumpumtabackend.rankbenchmark.dto.LatencyStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class RankingBenchmarkService {

    private static final Path OUTPUT_DIR = Paths.get("benchmark-results", "rank");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private final RankingBenchmarkProperties properties;
    private final ObjectMapper objectMapper;
    private final RankingResultVerifier verifier;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public BenchmarkReport run(BenchmarkRunRequest req) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("랭킹 벤치마크가 이미 실행 중입니다.");
        }
        try {
            return execute(req);
        } finally {
            running.set(false);
        }
    }

    private BenchmarkReport execute(BenchmarkRunRequest req) {
        int warmup = req != null && req.warmup() != null ? req.warmup() : properties.getHarness().getWarmupIterations();
        int iterations = req != null && req.iterations() != null ? req.iterations() : properties.getHarness().getMeasureIterations();
        Long userId = req != null ? req.userId() : null;
        String baseUrl = properties.getHarness().getBaseUrl();

        LocalDateTime startedAt = LocalDateTime.now();
        log.info("[RANK-BENCH] 시작 — warmup={} iter={} userId={} base={}", warmup, iterations, userId, baseUrl);

        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        List<Scenario> scenarios = List.of(
                Scenario.personal("personal-daily", "/personal/daily"),
                Scenario.personal("personal-weekly", "/personal/weekly"),
                Scenario.personal("personal-monthly", "/personal/monthly"),
                Scenario.department("department-daily", "/department/daily"),
                Scenario.department("department-weekly", "/department/weekly"),
                Scenario.department("department-monthly", "/department/monthly")
        );

        List<BenchmarkScenarioResult> results = new ArrayList<>(scenarios.size());
        for (Scenario scenario : scenarios) {
            BenchmarkScenarioResult r = measureScenario(client, scenario, warmup, iterations, userId);
            results.add(r);
            log.info("[RANK-BENCH] {} | mysql avg={}ms p95={}ms p99={}ms | redis avg={}ms p95={}ms p99={}ms | winner={} (x{})",
                    scenario.name,
                    fmt(r.mysql().avgMs()), fmt(r.mysql().p95Ms()), fmt(r.mysql().p99Ms()),
                    fmt(r.redis().avgMs()), fmt(r.redis().p95Ms()), fmt(r.redis().p99Ms()),
                    r.winner(), fmt(r.mysqlOverRedisRatio()));
        }

        LocalDateTime finishedAt = LocalDateTime.now();
        BenchmarkReport report = new BenchmarkReport("COMPLETED", warmup, iterations, userId, results, startedAt, finishedAt);
        writeResultFiles(report);
        return report;
    }

    private BenchmarkScenarioResult measureScenario(RestClient client, Scenario scenario,
                                                    int warmup, int iterations, Long userId) {
        String mysqlPath = "/api/v1/rank-benchmark/mysql" + scenario.pathSuffix;
        String redisPath = "/api/v1/rank-benchmark/redis" + scenario.pathSuffix;

        for (int i = 0; i < warmup; i++) {
            scenario.call(client, mysqlPath, userId);
            scenario.call(client, redisPath, userId);
        }
        scenario.verify(client, mysqlPath, redisPath, userId, verifier);

        List<Double> mysqlTimings = new ArrayList<>(iterations);
        List<Double> redisTimings = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            scenario.call(client, mysqlPath, userId);
            mysqlTimings.add((System.nanoTime() - t0) / 1_000_000.0);

            long t1 = System.nanoTime();
            scenario.call(client, redisPath, userId);
            redisTimings.add((System.nanoTime() - t1) / 1_000_000.0);
        }

        LatencyStats mysql = LatencyStats.from("MYSQL", mysqlTimings);
        LatencyStats redis = LatencyStats.from("REDIS", redisTimings);
        String winner = mysql.avgMs() <= redis.avgMs() ? "MYSQL" : "REDIS";
        double ratio = redis.avgMs() == 0 ? 0 : mysql.avgMs() / redis.avgMs();
        return new BenchmarkScenarioResult(scenario.name, mysql, redis, ratio, winner);
    }

    private void writeResultFiles(BenchmarkReport report) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            String stamp = report.startedAt().format(FILE_TS);
            Path jsonPath = OUTPUT_DIR.resolve("rank-bench-" + stamp + ".json");
            Path csvPath = OUTPUT_DIR.resolve("rank-bench-" + stamp + ".csv");

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), report);
            Files.writeString(csvPath, toCsv(report), StandardCharsets.UTF_8);

            log.info("[RANK-BENCH] 결과 저장 완료\n  json: {}\n  csv : {}",
                    jsonPath.toAbsolutePath(), csvPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[RANK-BENCH] 결과 파일 저장 실패", e);
        }
    }

    private String toCsv(BenchmarkReport report) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("scenario,")
          .append("mysql_avg_ms,mysql_p50_ms,mysql_p95_ms,mysql_p99_ms,mysql_min_ms,mysql_max_ms,")
          .append("redis_avg_ms,redis_p50_ms,redis_p95_ms,redis_p99_ms,redis_min_ms,redis_max_ms,")
          .append("winner,mysql_over_redis_ratio\n");
        for (BenchmarkScenarioResult e : report.scenarios()) {
            sb.append(e.scenario()).append(',')
              .append(num(e.mysql().avgMs())).append(',').append(num(e.mysql().p50Ms())).append(',')
              .append(num(e.mysql().p95Ms())).append(',').append(num(e.mysql().p99Ms())).append(',')
              .append(num(e.mysql().minMs())).append(',').append(num(e.mysql().maxMs())).append(',')
              .append(num(e.redis().avgMs())).append(',').append(num(e.redis().p50Ms())).append(',')
              .append(num(e.redis().p95Ms())).append(',').append(num(e.redis().p99Ms())).append(',')
              .append(num(e.redis().minMs())).append(',').append(num(e.redis().maxMs())).append(',')
              .append(e.winner()).append(',').append(num(e.mysqlOverRedisRatio())).append('\n');
        }
        return sb.toString();
    }

    private String num(double v) { return String.format(Locale.ROOT, "%.6f", v); }
    private String fmt(double v) { return String.format(Locale.ROOT, "%.3f", v); }

    public boolean isRunning() { return running.get(); }

    private record Scenario(String name, String pathSuffix, boolean personal) {
        static Scenario personal(String name, String suffix) { return new Scenario(name, suffix, true); }
        static Scenario department(String name, String suffix) { return new Scenario(name, suffix, false); }

        void call(RestClient client, String path, Long userId) {
            String uri = path + (userId != null ? "?userId=" + userId : "");
            if (personal) {
                client.get().uri(uri).retrieve().toEntity(PersonalRankingResponse.class);
            } else {
                client.get().uri(uri).retrieve().toEntity(DepartmentRankingResponse.class);
            }
        }

        void verify(RestClient client, String mysqlPath, String redisPath, Long userId, RankingResultVerifier verifier) {
            String uri = userId != null ? "?userId=" + userId : "";
            try {
                if (personal) {
                    PersonalRankingResponse m = client.get().uri(mysqlPath + uri).retrieve().body(PersonalRankingResponse.class);
                    PersonalRankingResponse r = client.get().uri(redisPath + uri).retrieve().body(PersonalRankingResponse.class);
                    if (m != null && r != null) verifier.comparePersonal(name, m, r);
                } else {
                    DepartmentRankingResponse m = client.get().uri(mysqlPath + uri).retrieve().body(DepartmentRankingResponse.class);
                    DepartmentRankingResponse r = client.get().uri(redisPath + uri).retrieve().body(DepartmentRankingResponse.class);
                    if (m != null && r != null) verifier.compareDepartment(name, m, r);
                }
            } catch (Exception e) {
                // verification is best-effort; never block the harness on it
            }
        }
    }
}
