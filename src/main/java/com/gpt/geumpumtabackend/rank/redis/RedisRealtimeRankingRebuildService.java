package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRealtimeRankingRebuildService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Duration RANKING_DAY_TTL = Duration.ofDays(3);
    private static final Duration RANKING_WEEK_TTL = Duration.ofDays(42);
    private static final Duration RANKING_MONTH_TTL = Duration.ofDays(183);
    private static final Duration ACTIVE_SESSION_TTL = Duration.ofHours(6);

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_START_AT_MS = "startAtMs";
    private static final String FIELD_DEPARTMENT = "department";

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public void rebuildCurrentPeriods() {
        LocalDate today = LocalDate.now();
        clearCurrentRankingKeys(today);

        for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
            rebuildPeriod(period, today);
        }

        rebuildActiveSessionMetadata();
        log.info("[REDIS_RANKING] Rebuilt current Redis ranking read model.");
    }

    private void rebuildPeriod(RedisRankingPeriod period, LocalDate date) {
        LocalDateTime periodStart = period.start(date);
        LocalDateTime periodEnd = period.nextStart(date);
        String periodId = period.id(date);

        for (UserScore score : completedScores(periodStart, periodEnd)) {
            addDoneScore(period, periodId, score.userId(), score.department(), score.totalMillis());
        }

        for (ActiveSession activeSession : activeSessions(periodStart, periodEnd)) {
            moveCurrentScoreToActive(period, periodId, activeSession);
        }
    }

    private void addDoneScore(
            RedisRankingPeriod period,
            String periodId,
            Long userId,
            Department department,
            Long totalMillis
    ) {
        if (totalMillis == null || totalMillis <= 0L) {
            return;
        }

        String member = userId.toString();
        redisTemplate.opsForZSet().incrementScore(RedisRankingKeys.userDone(period, periodId), member, totalMillis);
        redisTemplate.opsForZSet().incrementScore(
                RedisRankingKeys.departmentDone(department, period, periodId),
                member,
                totalMillis
        );
        expirePeriodKeys(period, periodId, department);
    }

    private void moveCurrentScoreToActive(
            RedisRankingPeriod period,
            String periodId,
            ActiveSession activeSession
    ) {
        String member = activeSession.userId().toString();
        long contributionStartMs = toEpochMs(max(activeSession.startTime(), period.start(LocalDate.now())));

        moveOneKeyToActive(
                RedisRankingKeys.userDone(period, periodId),
                RedisRankingKeys.userActive(period, periodId),
                member,
                contributionStartMs
        );
        moveOneKeyToActive(
                RedisRankingKeys.departmentDone(activeSession.department(), period, periodId),
                RedisRankingKeys.departmentActive(activeSession.department(), period, periodId),
                member,
                contributionStartMs
        );
        expirePeriodKeys(period, periodId, activeSession.department());
    }

    private void moveOneKeyToActive(String doneKey, String activeKey, String member, long contributionStartMs) {
        double baseScore = score(doneKey, member);
        redisTemplate.opsForZSet().remove(doneKey, member);
        redisTemplate.opsForZSet().add(activeKey, member, baseScore - contributionStartMs);
    }

    private void rebuildActiveSessionMetadata() {
        List<ActiveSession> activeSessions = activeSessions(LocalDate.now().atStartOfDay(), RedisRankingPeriod.MONTH.nextStart(LocalDate.now()));
        for (ActiveSession activeSession : activeSessions) {
            String member = activeSession.userId().toString();
            Map<String, String> metadata = new HashMap<>();
            metadata.put(FIELD_SESSION_ID, activeSession.sessionId().toString());
            metadata.put(FIELD_START_AT_MS, Long.toString(toEpochMs(activeSession.startTime())));
            metadata.put(FIELD_DEPARTMENT, activeSession.department().name());

            redisTemplate.opsForHash().putAll(RedisRankingKeys.activeSession(activeSession.userId()), metadata);
            redisTemplate.expire(RedisRankingKeys.activeSession(activeSession.userId()), ACTIVE_SESSION_TTL);
            redisTemplate.opsForSet().add(RedisRankingKeys.ACTIVE_USERS, member);
        }
    }

    private List<UserScore> completedScores(LocalDateTime periodStart, LocalDateTime periodEnd) {
        String sql = """
                SELECT u.id AS user_id,
                       u.department AS department,
                       CAST(FLOOR(SUM(
                           TIMESTAMPDIFF(MICROSECOND,
                               GREATEST(s.start_time, ?),
                               LEAST(s.end_time, ?)
                           )
                       ) / 1000) AS SIGNED) AS total_millis
                FROM study_session s
                JOIN `user` u ON u.id = s.user_id
                WHERE u.role = 'USER'
                  AND u.department IS NOT NULL
                  AND s.end_time IS NOT NULL
                  AND s.start_time < ?
                  AND s.end_time > ?
                GROUP BY u.id, u.department
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Department department = parseDepartment(rs.getString("department"));
            if (department == null) {
                return null;
            }
            return new UserScore(rs.getLong("user_id"), department, rs.getLong("total_millis"));
        }, Timestamp.valueOf(periodStart), Timestamp.valueOf(periodEnd), Timestamp.valueOf(periodEnd), Timestamp.valueOf(periodStart))
                .stream()
                .filter(score -> score != null && score.totalMillis() > 0L)
                .toList();
    }

    private List<ActiveSession> activeSessions(LocalDateTime periodStart, LocalDateTime periodEnd) {
        String sql = """
                SELECT s.id AS session_id,
                       u.id AS user_id,
                       u.department AS department,
                       s.start_time AS start_time
                FROM study_session s
                JOIN `user` u ON u.id = s.user_id
                WHERE u.role = 'USER'
                  AND u.department IS NOT NULL
                  AND s.status = 'STARTED'
                  AND s.end_time IS NULL
                  AND s.start_time < ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Department department = parseDepartment(rs.getString("department"));
            if (department == null) {
                return null;
            }
            Timestamp startTime = rs.getTimestamp("start_time");
            if (startTime == null || !startTime.toLocalDateTime().isBefore(periodEnd)) {
                return null;
            }
            return new ActiveSession(
                    rs.getLong("session_id"),
                    rs.getLong("user_id"),
                    department,
                    startTime.toLocalDateTime()
            );
        }, Timestamp.valueOf(periodEnd))
                .stream()
                .filter(session -> session != null && session.startTime().isBefore(periodEnd))
                .filter(session -> session.startTime().isAfter(periodStart) || session.startTime().isEqual(periodStart) || session.startTime().isBefore(periodEnd))
                .toList();
    }

    private void clearCurrentRankingKeys(LocalDate date) {
        List<String> keys = new ArrayList<>();
        for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
            String periodId = period.id(date);
            keys.add(RedisRankingKeys.userDone(period, periodId));
            keys.add(RedisRankingKeys.userActive(period, periodId));
            for (Department department : Department.values()) {
                keys.add(RedisRankingKeys.departmentDone(department, period, periodId));
                keys.add(RedisRankingKeys.departmentActive(department, period, periodId));
            }
        }

        var activeUsers = redisTemplate.opsForSet().members(RedisRankingKeys.ACTIVE_USERS);
        if (activeUsers != null) {
            for (Object rawUserId : activeUsers) {
                Long userId = parseLong(rawUserId);
                if (userId != null) {
                    keys.add(RedisRankingKeys.activeSession(userId));
                }
            }
        }
        keys.add(RedisRankingKeys.ACTIVE_USERS);

        redisTemplate.delete(keys);
    }

    private void expirePeriodKeys(RedisRankingPeriod period, String periodId, Department department) {
        Duration ttl = ttl(period);
        redisTemplate.expire(RedisRankingKeys.userDone(period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.userActive(period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.departmentDone(department, period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.departmentActive(department, period, periodId), ttl);
    }

    private double score(String key, String member) {
        Double score = redisTemplate.opsForZSet().score(key, member);
        return score == null ? 0D : score;
    }

    private Duration ttl(RedisRankingPeriod period) {
        return switch (period) {
            case DAY -> RANKING_DAY_TTL;
            case WEEK -> RANKING_WEEK_TTL;
            case MONTH -> RANKING_MONTH_TTL;
        };
    }

    private long toEpochMs(LocalDateTime time) {
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private Department parseDepartment(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Department.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("[REDIS_RANKING] Unknown department while rebuilding ranking. department={}", value);
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record UserScore(Long userId, Department department, Long totalMillis) {
    }

    private record ActiveSession(Long sessionId, Long userId, Department department, LocalDateTime startTime) {
    }
}
