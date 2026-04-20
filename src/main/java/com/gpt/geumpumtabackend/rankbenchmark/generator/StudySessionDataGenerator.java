package com.gpt.geumpumtabackend.rankbenchmark.generator;

import com.gpt.geumpumtabackend.rankbenchmark.RankingBenchmarkProperties;
import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class StudySessionDataGenerator {

    private static final String USER_EMAIL_PREFIX = "rankbench_";

    private final JdbcTemplate jdbcTemplate;
    private final RankingBenchmarkProperties properties;

    public GenerateReport generate(Long totalSessionsOverride, Integer userCountOverride, Integer startedOverride) {
        long totalSessions = totalSessionsOverride != null ? totalSessionsOverride : properties.getDataset().getTotalSessions();
        int userCount = userCountOverride != null ? userCountOverride : properties.getDataset().getUserCount();
        int startedCount = startedOverride != null ? startedOverride : properties.getDataset().getStartedSessionCount();
        int batchChunk = properties.getDataset().getBatchChunk();

        long t0 = System.nanoTime();
        List<Long> userIds = ensureUsers(userCount, batchChunk);
        long userLoadMs = (System.nanoTime() - t0) / 1_000_000;

        deleteExistingBenchmarkSessions(userIds);

        long sessionT0 = System.nanoTime();
        long finishedInserted = insertFinishedSessions(userIds, totalSessions - startedCount, batchChunk);
        long startedInserted = insertStartedSessions(userIds, startedCount);
        long sessionLoadMs = (System.nanoTime() - sessionT0) / 1_000_000;

        log.info("[RANK-BENCH][GEN] users={} finished={} started={} userLoadMs={} sessionLoadMs={}",
                userIds.size(), finishedInserted, startedInserted, userLoadMs, sessionLoadMs);

        return new GenerateReport(userIds.size(), finishedInserted, startedInserted, userLoadMs, sessionLoadMs);
    }

    private List<Long> ensureUsers(int targetCount, int batchChunk) {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT id FROM user WHERE email LIKE ? AND deleted_at IS NULL ORDER BY id",
                Long.class, USER_EMAIL_PREFIX + "%");
        if (existing.size() >= targetCount) {
            return existing.subList(0, targetCount);
        }

        Department[] deps = Department.values();
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batch = new ArrayList<>(batchChunk);
        int needed = targetCount - existing.size();
        int startIndex = existing.size();

        for (int i = 0; i < needed; i++) {
            int idx = startIndex + i;
            Department dep = deps[idx % deps.length];
            String tag = String.valueOf(System.currentTimeMillis() % 100000) + "_" + idx;
            batch.add(new Object[]{
                    USER_EMAIL_PREFIX + tag + "@example.com",
                    USER_EMAIL_PREFIX + tag + "@kumoh.ac.kr",
                    "USER",
                    "bench_" + tag,
                    "bench_" + tag,
                    dep.name(),
                    "BENCH" + String.format("%07d", idx),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now)
            });
            if (batch.size() == batchChunk) {
                flushUserBatch(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) flushUserBatch(batch);

        return jdbcTemplate.queryForList(
                "SELECT id FROM user WHERE email LIKE ? AND deleted_at IS NULL ORDER BY id",
                Long.class, USER_EMAIL_PREFIX + "%").subList(0, targetCount);
    }

    private void flushUserBatch(List<Object[]> batch) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO user (email, school_email, role, name, nickname, department, student_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batch);
    }

    private void deleteExistingBenchmarkSessions(List<Long> userIds) {
        if (userIds.isEmpty()) return;
        int chunk = 1000;
        for (int i = 0; i < userIds.size(); i += chunk) {
            List<Long> slice = userIds.subList(i, Math.min(i + chunk, userIds.size()));
            String placeholders = String.join(",", slice.stream().map(x -> "?").toList());
            jdbcTemplate.update(
                    "DELETE FROM study_session WHERE user_id IN (" + placeholders + ")",
                    slice.toArray());
        }
    }

    private long insertFinishedSessions(List<Long> userIds, long totalSessions, int batchChunk) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long todayPortion = Math.round(totalSessions * 0.60);
        long weekPortion = Math.round(totalSessions * 0.25);
        long monthPortion = totalSessions - todayPortion - weekPortion;

        long inserted = 0;
        inserted += insertBucket(userIds, todayPortion, startOfToday, endOfToday, batchChunk);
        inserted += insertBucket(userIds, weekPortion, weekStart, startOfToday.minusSeconds(1), batchChunk);
        inserted += insertBucket(userIds, monthPortion, monthStart, weekStart.minusSeconds(1), batchChunk);
        return inserted;
    }

    private long insertBucket(List<Long> userIds, long count, LocalDateTime lower, LocalDateTime upper, int batchChunk) {
        if (count <= 0) return 0;
        long lowerEpoch = toEpochSecond(lower);
        long upperEpoch = toEpochSecond(upper);
        if (upperEpoch <= lowerEpoch) upperEpoch = lowerEpoch + 60;

        List<Object[]> batch = new ArrayList<>(batchChunk);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long inserted = 0;
        int userSize = userIds.size();

        for (long i = 0; i < count; i++) {
            long userId = userIds.get(rng.nextInt(userSize));
            long startEpoch = rng.nextLong(lowerEpoch, upperEpoch);
            long durationSec = rng.nextLong(60, 3 * 60 * 60);
            long endEpoch = Math.min(startEpoch + durationSec, upperEpoch);
            long totalMillis = (endEpoch - startEpoch) * 1000L;

            LocalDateTime startTime = LocalDateTime.ofEpochSecond(startEpoch, 0, java.time.ZoneOffset.UTC);
            LocalDateTime endTime = LocalDateTime.ofEpochSecond(endEpoch, 0, java.time.ZoneOffset.UTC);

            batch.add(new Object[]{
                    Timestamp.valueOf(startTime),
                    Timestamp.valueOf(endTime),
                    totalMillis,
                    "FINISHED",
                    userId
            });

            if (batch.size() == batchChunk) {
                flushSessionBatch(batch);
                inserted += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            flushSessionBatch(batch);
            inserted += batch.size();
        }
        return inserted;
    }

    private long insertStartedSessions(List<Long> userIds, int count) {
        if (count <= 0) return 0;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long userId = userIds.get(rng.nextInt(userIds.size()));
            LocalDateTime startTime = now.minusMinutes(rng.nextInt(1, 170));
            batch.add(new Object[]{
                    Timestamp.valueOf(startTime),
                    null,
                    null,
                    "STARTED",
                    userId
            });
        }
        flushSessionBatch(batch);
        return batch.size();
    }

    private void flushSessionBatch(List<Object[]> batch) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO study_session (start_time, end_time, total_millis, status, user_id) " +
                        "VALUES (?, ?, ?, ?, ?)",
                batch);
    }

    private long toEpochSecond(LocalDateTime dt) {
        return dt.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    public record GenerateReport(
            int users,
            long finishedSessions,
            long startedSessions,
            long userLoadMs,
            long sessionLoadMs
    ) {}
}
