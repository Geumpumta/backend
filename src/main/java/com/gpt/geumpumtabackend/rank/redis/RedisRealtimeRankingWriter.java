package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRealtimeRankingWriter {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Duration RANKING_DAY_TTL = Duration.ofDays(3);
    private static final Duration RANKING_WEEK_TTL = Duration.ofDays(42);
    private static final Duration RANKING_MONTH_TTL = Duration.ofDays(183);
    private static final Duration ACTIVE_SESSION_TTL = Duration.ofHours(6);

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_START_AT_MS = "startAtMs";
    private static final String FIELD_DEPARTMENT = "department";

    private final RedisTemplate<String, Object> redisTemplate;

    public void recordSessionStarted(RedisRankingSessionSnapshot session) {
        if (!hasRequiredStartData(session)) {
            return;
        }

        try {
            String userId = session.userId().toString();
            long startAtMs = toEpochMs(session.startTime());
            Map<String, String> metadata = new HashMap<>();
            metadata.put(FIELD_SESSION_ID, session.sessionId().toString());
            metadata.put(FIELD_START_AT_MS, Long.toString(startAtMs));
            metadata.put(FIELD_DEPARTMENT, session.department().name());

            for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
                String periodId = period.id(session.startTime().toLocalDate());
                moveToActive(
                        RedisRankingKeys.userDone(period, periodId),
                        RedisRankingKeys.userActive(period, periodId),
                        userId,
                        startAtMs,
                        ttl(period)
                );
                moveToActive(
                        RedisRankingKeys.departmentDone(session.department(), period, periodId),
                        RedisRankingKeys.departmentActive(session.department(), period, periodId),
                        userId,
                        startAtMs,
                        ttl(period)
                );
            }

            redisTemplate.opsForHash().putAll(RedisRankingKeys.activeSession(session.userId()), metadata);
            redisTemplate.expire(RedisRankingKeys.activeSession(session.userId()), ACTIVE_SESSION_TTL);
            redisTemplate.opsForSet().add(RedisRankingKeys.ACTIVE_USERS, userId);
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to record session start. sessionId={}, userId={}",
                    session.sessionId(), session.userId(), e);
        }
    }

    public void recordSessionEnded(RedisRankingSessionSnapshot session) {
        if (!hasRequiredEndData(session)) {
            return;
        }

        try {
            String activeHashKey = RedisRankingKeys.activeSession(session.userId());
            Object redisSessionId = redisTemplate.opsForHash().get(activeHashKey, FIELD_SESSION_ID);
            if (redisSessionId == null || !session.sessionId().toString().equals(redisSessionId.toString())) {
                log.warn("[REDIS_RANKING] Skip end update because active metadata is missing or mismatched. sessionId={}, userId={}",
                        session.sessionId(), session.userId());
                return;
            }

            String userId = session.userId().toString();
            for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
                applyFinishedSegments(session, period, userId, null);
                applyFinishedSegments(session, period, userId, session.department());
            }

            redisTemplate.opsForSet().remove(RedisRankingKeys.ACTIVE_USERS, userId);
            redisTemplate.delete(activeHashKey);
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to record session end. sessionId={}, userId={}",
                    session.sessionId(), session.userId(), e);
        }
    }

    public void rollActiveSessionsToCurrentPeriods() {
        try {
            var activeUsers = redisTemplate.opsForSet().members(RedisRankingKeys.ACTIVE_USERS);
            if (activeUsers == null || activeUsers.isEmpty()) {
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            long todayStartMs = toEpochMs(today.atStartOfDay());

            for (Object rawUserId : activeUsers) {
                Long userId = parseLong(rawUserId);
                if (userId == null) {
                    continue;
                }
                Department department = activeDepartment(userId);
                if (department == null) {
                    continue;
                }

                rollPeriod(userId, department, RedisRankingPeriod.DAY, yesterday, today, todayStartMs);
                if (today.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                    rollPeriod(userId, department, RedisRankingPeriod.WEEK, yesterday, today, todayStartMs);
                }
                if (today.getDayOfMonth() == 1) {
                    rollPeriod(userId, department, RedisRankingPeriod.MONTH, yesterday, today, todayStartMs);
                }
            }
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to roll active sessions to current period.", e);
        }
    }

    private void moveToActive(String doneKey, String activeKey, String userId, long contributionStartMs, Duration ttl) {
        if (redisTemplate.opsForZSet().score(activeKey, userId) != null) {
            return;
        }

        double baseScore = score(doneKey, userId);
        redisTemplate.opsForZSet().remove(doneKey, userId);
        redisTemplate.opsForZSet().add(activeKey, userId, baseScore - contributionStartMs);
        redisTemplate.expire(doneKey, ttl);
        redisTemplate.expire(activeKey, ttl);
    }

    private void applyFinishedSegments(
            RedisRankingSessionSnapshot session,
            RedisRankingPeriod period,
            String userId,
            Department department
    ) {
        LocalDateTime cursor = session.startTime();
        while (cursor.isBefore(session.endTime())) {
            LocalDate cursorDate = cursor.toLocalDate();
            LocalDateTime periodEnd = period.nextStart(cursorDate);
            LocalDateTime segmentEnd = min(periodEnd, session.endTime());
            String periodId = period.id(cursorDate);
            long segmentEndMs = toEpochMs(segmentEnd);
            long segmentMillis = Math.max(0L, segmentEndMs - toEpochMs(cursor));

            String doneKey = department == null
                    ? RedisRankingKeys.userDone(period, periodId)
                    : RedisRankingKeys.departmentDone(department, period, periodId);
            String activeKey = department == null
                    ? RedisRankingKeys.userActive(period, periodId)
                    : RedisRankingKeys.departmentActive(department, period, periodId);

            Double activeScore = redisTemplate.opsForZSet().score(activeKey, userId);
            if (activeScore != null) {
                redisTemplate.opsForZSet().remove(activeKey, userId);
                redisTemplate.opsForZSet().add(doneKey, userId, activeScore + segmentEndMs);
            } else if (segmentMillis > 0) {
                redisTemplate.opsForZSet().incrementScore(doneKey, userId, segmentMillis);
            }
            redisTemplate.expire(doneKey, ttl(period));
            redisTemplate.expire(activeKey, ttl(period));

            cursor = segmentEnd;
        }
    }

    private void rollPeriod(
            Long userId,
            Department department,
            RedisRankingPeriod period,
            LocalDate previousDate,
            LocalDate currentDate,
            long boundaryMs
    ) {
        String userIdValue = userId.toString();
        rollOneKey(
                RedisRankingKeys.userDone(period, period.id(previousDate)),
                RedisRankingKeys.userActive(period, period.id(previousDate)),
                RedisRankingKeys.userDone(period, period.id(currentDate)),
                RedisRankingKeys.userActive(period, period.id(currentDate)),
                userIdValue,
                boundaryMs,
                ttl(period)
        );
        rollOneKey(
                RedisRankingKeys.departmentDone(department, period, period.id(previousDate)),
                RedisRankingKeys.departmentActive(department, period, period.id(previousDate)),
                RedisRankingKeys.departmentDone(department, period, period.id(currentDate)),
                RedisRankingKeys.departmentActive(department, period, period.id(currentDate)),
                userIdValue,
                boundaryMs,
                ttl(period)
        );
    }

    private void rollOneKey(
            String previousDoneKey,
            String previousActiveKey,
            String currentDoneKey,
            String currentActiveKey,
            String userId,
            long boundaryMs,
            Duration ttl
    ) {
        Double previousActiveScore = redisTemplate.opsForZSet().score(previousActiveKey, userId);
        if (previousActiveScore == null) {
            return;
        }

        redisTemplate.opsForZSet().remove(previousActiveKey, userId);
        redisTemplate.opsForZSet().add(previousDoneKey, userId, previousActiveScore + boundaryMs);

        double currentBase = score(currentDoneKey, userId);
        redisTemplate.opsForZSet().remove(currentDoneKey, userId);
        redisTemplate.opsForZSet().add(currentActiveKey, userId, currentBase - boundaryMs);

        redisTemplate.expire(previousDoneKey, ttl);
        redisTemplate.expire(previousActiveKey, ttl);
        redisTemplate.expire(currentDoneKey, ttl);
        redisTemplate.expire(currentActiveKey, ttl);
    }

    private Department activeDepartment(Long userId) {
        Object value = redisTemplate.opsForHash().get(RedisRankingKeys.activeSession(userId), FIELD_DEPARTMENT);
        if (value == null) {
            return null;
        }
        try {
            return Department.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean hasRequiredStartData(RedisRankingSessionSnapshot session) {
        return session != null
                && session.sessionId() != null
                && session.userId() != null
                && session.department() != null
                && session.startTime() != null;
    }

    private boolean hasRequiredEndData(RedisRankingSessionSnapshot session) {
        return hasRequiredStartData(session)
                && session.endTime() != null
                && session.totalMillis() != null;
    }

    private double score(String key, String userId) {
        Double score = redisTemplate.opsForZSet().score(key, userId);
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

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
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
}
