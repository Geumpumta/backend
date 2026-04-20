package com.gpt.geumpumtabackend.rankbenchmark.redis;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("local")
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class RedisRankingBackfillService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public BackfillReport backfillAll() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);

        clearExistingKeys();

        long t0 = System.nanoTime();
        int dayCount = loadPeriod(RedisRankingKeys.userDay(today),
                department -> RedisRankingKeys.userDeptDay(department, today),
                startOfToday, endOfToday);

        int weekCount = loadPeriod(RedisRankingKeys.userWeek(today),
                department -> RedisRankingKeys.userDeptWeek(department, today),
                weekStart, weekEnd);

        int monthCount = loadPeriod(RedisRankingKeys.userMonth(today),
                department -> RedisRankingKeys.userDeptMonth(department, today),
                monthStart, monthEnd);

        int activeCount = loadActiveSessions();
        long totalMs = (System.nanoTime() - t0) / 1_000_000;

        log.info("[RANK-BENCH][BACKFILL] day={} week={} month={} active={} totalMs={}",
                dayCount, weekCount, monthCount, activeCount, totalMs);

        return new BackfillReport(dayCount, weekCount, monthCount, activeCount, totalMs);
    }

    private int loadPeriod(String globalKey, DepartmentKeyFn deptKeyFn,
                           LocalDateTime periodStart, LocalDateTime periodEnd) {
        String sql = """
                SELECT u.id as user_id, u.department as department,
                       CAST(FLOOR(SUM(
                           TIMESTAMPDIFF(MICROSECOND,
                               GREATEST(s.start_time, ?),
                               LEAST(s.end_time, ?)
                           ) / 1000
                       )) AS SIGNED) as total_ms
                FROM user u JOIN study_session s ON u.id = s.user_id
                WHERE u.role = 'USER'
                  AND u.department IS NOT NULL
                  AND s.status = 'FINISHED'
                  AND s.start_time <= ?
                  AND s.end_time >= ?
                GROUP BY u.id, u.department
                HAVING total_ms > 0
                """;

        int[] written = {0};
        byte[] globalKeyBytes = globalKey.getBytes(StandardCharsets.UTF_8);
        Map<String, Map<Long, Double>> deptBuckets = new HashMap<>();
        Map<Long, Double> globalBucket = new HashMap<>();

        jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setObject(1, periodStart);
            ps.setObject(2, periodEnd);
            ps.setObject(3, periodEnd);
            ps.setObject(4, periodStart);
            return ps;
        }, rs -> {
            long userId = rs.getLong("user_id");
            String dep = rs.getString("department");
            double score = rs.getDouble("total_ms");
            globalBucket.put(userId, score);
            deptBuckets.computeIfAbsent(dep, k -> new HashMap<>()).put(userId, score);

            if (globalBucket.size() >= 5_000) {
                flushBucket(globalKeyBytes, globalBucket);
                for (Map.Entry<String, Map<Long, Double>> e : deptBuckets.entrySet()) {
                    Department dept = Department.valueOf(e.getKey());
                    flushBucket(deptKeyFn.apply(dept).getBytes(StandardCharsets.UTF_8), e.getValue());
                }
                written[0] += globalBucket.size();
                globalBucket.clear();
                deptBuckets.clear();
            }
        });

        if (!globalBucket.isEmpty()) {
            flushBucket(globalKeyBytes, globalBucket);
            for (Map.Entry<String, Map<Long, Double>> e : deptBuckets.entrySet()) {
                Department dept = Department.valueOf(e.getKey());
                flushBucket(deptKeyFn.apply(dept).getBytes(StandardCharsets.UTF_8), e.getValue());
            }
            written[0] += globalBucket.size();
        }

        return written[0];
    }

    private void flushBucket(byte[] keyBytes, Map<Long, Double> bucket) {
        redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            for (Map.Entry<Long, Double> e : bucket.entrySet()) {
                conn.zSetCommands().zAdd(keyBytes,
                        e.getValue(),
                        e.getKey().toString().getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
    }

    private int loadActiveSessions() {
        String sql = """
                SELECT s.id as session_id, s.user_id as user_id, s.start_time as start_time, u.department as department
                FROM study_session s JOIN user u ON u.id = s.user_id
                WHERE s.status = 'STARTED' AND u.role = 'USER'
                """;

        Set<String> userIds = new HashSet<>();
        Map<Long, Object[]> userMeta = new HashMap<>();
        Map<Long, Long> userExpiry = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            long sessionId = rs.getLong("session_id");
            long userId = rs.getLong("user_id");
            LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
            String department = rs.getString("department");
            long startAtEpochMs = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            userIds.add(Long.toString(userId));
            userMeta.put(userId, new Object[]{sessionId, startAtEpochMs, department});
            userExpiry.put(userId, startAtEpochMs + 3L * 60L * 60L * 1000L);
        });

        if (userIds.isEmpty()) return 0;

        redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            byte[] usersKey = RedisRankingKeys.ACTIVE_USERS_SET.getBytes(StandardCharsets.UTF_8);
            byte[] expiryKey = RedisRankingKeys.ACTIVE_EXPIRY_ZSET.getBytes(StandardCharsets.UTF_8);

            for (Map.Entry<Long, Object[]> entry : userMeta.entrySet()) {
                long uid = entry.getKey();
                Object[] meta = entry.getValue();
                byte[] userIdBytes = Long.toString(uid).getBytes(StandardCharsets.UTF_8);
                byte[] hashKey = RedisRankingKeys.activeSessionHash(uid).getBytes(StandardCharsets.UTF_8);

                Map<byte[], byte[]> hash = new HashMap<>();
                hash.put(ActiveSessionMetadata.FIELD_SESSION_ID.getBytes(StandardCharsets.UTF_8),
                        meta[0].toString().getBytes(StandardCharsets.UTF_8));
                hash.put(ActiveSessionMetadata.FIELD_START_AT.getBytes(StandardCharsets.UTF_8),
                        meta[1].toString().getBytes(StandardCharsets.UTF_8));
                hash.put(ActiveSessionMetadata.FIELD_DEPARTMENT.getBytes(StandardCharsets.UTF_8),
                        ((String) meta[2]).getBytes(StandardCharsets.UTF_8));
                conn.hashCommands().hMSet(hashKey, hash);
                conn.setCommands().sAdd(usersKey, userIdBytes);
                conn.zSetCommands().zAdd(expiryKey, userExpiry.get(uid), userIdBytes);
            }
            return null;
        });

        return userMeta.size();
    }

    private void clearExistingKeys() {
        Set<String> toDelete = new HashSet<>();
        toDelete.add(RedisRankingKeys.ACTIVE_USERS_SET);
        toDelete.add(RedisRankingKeys.ACTIVE_EXPIRY_ZSET);

        scanAndCollect("rank:user:*", toDelete);
        scanAndCollect(RedisRankingKeys.ACTIVE_SESSION_HASH + "*", toDelete);

        if (!toDelete.isEmpty()) {
            redisTemplate.delete(toDelete);
        }
    }

    private void scanAndCollect(String pattern, Set<String> sink) {
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                (RedisCallback<Cursor<byte[]>>) conn ->
                        conn.keyCommands().scan(ScanOptions.scanOptions().match(pattern).count(1000).build()))) {
            if (cursor == null) return;
            while (cursor.hasNext()) {
                sink.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
    }

    @FunctionalInterface
    private interface DepartmentKeyFn {
        String apply(Department department);
    }

    public record BackfillReport(
            int dayUsers,
            int weekUsers,
            int monthUsers,
            int activeSessions,
            long totalMs
    ) {}
}
