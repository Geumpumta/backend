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
/**
 * Redis 실시간 랭킹 조회 모델을 쓰는 컴포넌트.
 *
 * 원본 데이터는 MySQL이고, Redis는 빠른 랭킹 조회를 위한 파생 조회 모델이다.
 * 따라서 Redis 쓰기가 실패해도 사용자 요청을 실패시키지 않고 로그만 남긴다.
 *
 * 핵심 아이디어:
 * - 공부를 끝낸 사용자는 done ZSET에 저장한다. score = 완료된 누적 공부 시간(ms).
 * - 공부 중인 사용자는 active ZSET에 저장한다.
 *   score = 공부 시작 전 누적 공부 시간(ms) - 공부 반영 시작 시각(epoch ms).
 * - reader는 active score에 현재 시각(epoch ms)을 더해서 현재 점수를 계산한다.
 *
 * 이 방식으로 1초마다 Redis에 쓰지 않아도, 조회 시점에는 점수가 매초 증가하는 것처럼 계산된다.
 */
public class RedisRealtimeRankingWriter {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    // 랭킹 key는 임시 read model이다. 오래된 기간 key는 필요 기간이 지나면 TTL로 제거한다.
    private static final Duration RANKING_DAY_TTL = Duration.ofDays(3);
    private static final Duration RANKING_WEEK_TTL = Duration.ofDays(42);
    private static final Duration RANKING_MONTH_TTL = Duration.ofDays(183);
    // 활성 세션 메타데이터는 비정상 종료 후에도 영구히 남으면 안 되므로 TTL을 둔다.
    private static final Duration ACTIVE_SESSION_TTL = Duration.ofHours(6);

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_START_AT_MS = "startAtMs";
    private static final String FIELD_DEPARTMENT = "department";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 공부 시작 트랜잭션이 커밋된 뒤 호출된다.
     *
     * 일간/주간/월간 각각에 대해 사용자를 done에서 active로 이동한다.
     * active 점수 = 기존 완료 점수 - 세션 시작 시각(epoch ms).
     *
     * 예를 들어 기존 완료 점수가 30분이고 시작 시각이 T라면,
     * reader는 기존 완료 점수 - T + now = 기존 완료 점수 + 경과 시간으로 현재 점수를 계산한다.
     */
    public void recordSessionStarted(RedisRankingSessionSnapshot session) {
        if (!hasRequiredStartData(session)) {
            // 필수 값이 없으면 Redis 랭킹 정합성을 보장할 수 없으므로 아무 작업도 하지 않는다.
            return;
        }

        try {
            String userId = session.userId().toString();
            long startAtMs = toEpochMs(session.startTime());
            // 종료 이벤트 검증과 기간 경계 처리에 필요한 최소 메타데이터를 hash로 저장한다.
            Map<String, String> metadata = new HashMap<>();
            metadata.put(FIELD_SESSION_ID, session.sessionId().toString());
            metadata.put(FIELD_START_AT_MS, Long.toString(startAtMs));
            metadata.put(FIELD_DEPARTMENT, session.department().name());

            for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
                // 세션 시작 시각이 속한 일간/주간/월간 key를 각각 계산한다.
                String periodId = period.id(session.startTime().toLocalDate());
                // 개인 랭킹 key에서 done 점수를 active 보정 점수로 이동한다.
                moveToActive(
                        RedisRankingKeys.userDone(period, periodId),
                        RedisRankingKeys.userActive(period, periodId),
                        userId,
                        startAtMs,
                        ttl(period)
                );
                // 학과 랭킹 key도 개인 랭킹과 같은 방식으로 이동한다.
                moveToActive(
                        RedisRankingKeys.departmentDone(session.department(), period, periodId),
                        RedisRankingKeys.departmentActive(session.department(), period, periodId),
                        userId,
                        startAtMs,
                        ttl(period)
                );
            }

            // 나중에 들어오는 종료 이벤트가 현재 활성 세션과 같은 세션인지 검증하기 위한 메타데이터를 저장한다.
            redisTemplate.opsForHash().putAll(RedisRankingKeys.activeSession(session.userId()), metadata);
            redisTemplate.expire(RedisRankingKeys.activeSession(session.userId()), ACTIVE_SESSION_TTL);
            // 자정/주간/월간 경계 처리 대상자를 찾기 위해 활성 사용자 set을 유지한다.
            redisTemplate.opsForSet().add(RedisRankingKeys.ACTIVE_USERS, userId);
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to record session start. sessionId={}, userId={}",
                    session.sessionId(), session.userId(), e);
        }
    }

    /**
     * 공부 종료 트랜잭션이 커밋된 뒤 호출된다.
     *
     * 공부 중 ZSET의 사용자를 완료 ZSET으로 확정한다.
     * 세션이 일/주/月 경계를 넘었다면 여러 기간 세그먼트로 나눠 반영한다.
     */
    public void recordSessionEnded(RedisRankingSessionSnapshot session) {
        if (!hasRequiredEndData(session)) {
            // 종료 시간과 총 공부 시간이 없으면 확정 점수를 만들 수 없으므로 무시한다.
            return;
        }

        try {
            String activeHashKey = RedisRankingKeys.activeSession(session.userId());
            // Redis에 저장된 활성 세션 id를 읽어 현재 종료되는 세션과 같은지 확인한다.
            Object redisSessionId = redisTemplate.opsForHash().get(activeHashKey, FIELD_SESSION_ID);
            // Redis가 시작 이벤트를 놓쳤거나 다른 활성 세션과 충돌한 경우에는 추측으로 쓰지 않는다.
            // MySQL은 여전히 정합한 원본이고, Redis는 rebuildCurrentPeriods()로 복구할 수 있다.
            if (redisSessionId == null || !session.sessionId().toString().equals(redisSessionId.toString())) {
                log.warn("[REDIS_RANKING] Skip end update because active metadata is missing or mismatched. sessionId={}, userId={}",
                        session.sessionId(), session.userId());
                return;
            }

            String userId = session.userId().toString();
            for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
                // 개인 랭킹에 세션 종료 점수를 반영한다.
                applyFinishedSegments(session, period, userId, null);
                // 학과 랭킹에도 같은 세션 종료 점수를 반영한다.
                applyFinishedSegments(session, period, userId, session.department());
            }

            // 종료된 사용자는 더 이상 active 사용자 목록과 active metadata에 남기지 않는다.
            redisTemplate.opsForSet().remove(RedisRankingKeys.ACTIVE_USERS, userId);
            redisTemplate.delete(activeHashKey);
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to record session end. sessionId={}, userId={}",
                    session.sessionId(), session.userId(), e);
        }
    }

    /**
     * 날짜/주/月 경계에서 active 세션을 새 기간 key로 넘긴다.
     *
     * 조회기는 기간별 Redis 키를 조회하므로, 자정을 넘겨 공부 중인 사용자는
     * 어제 완료 키에는 00:00까지의 점수로 확정되어야 하고,
     * 오늘 공부 중 키에는 00:00부터 다시 시작한 형태로 들어가야 한다.
     */
    public void rollActiveSessionsToCurrentPeriods() {
        try {
            var activeUsers = redisTemplate.opsForSet().members(RedisRankingKeys.ACTIVE_USERS);
            if (activeUsers == null || activeUsers.isEmpty()) {
                // 공부 중인 사용자가 없으면 경계 처리할 key도 없다.
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            // 모든 경계 처리는 오늘 00:00:00을 기준으로 이전 기간과 현재 기간을 나눈다.
            long todayStartMs = toEpochMs(today.atStartOfDay());

            for (Object rawUserId : activeUsers) {
                Long userId = parseLong(rawUserId);
                if (userId == null) {
                    // 잘못된 userId 값은 건너뛴다.
                    continue;
                }
                Department department = activeDepartment(userId);
                if (department == null) {
                    // 학과 정보가 없으면 학과 key를 계산할 수 없으므로 건너뛴다.
                    continue;
                }

                // 일간 key는 매일 자정에 바뀐다.
                rollPeriod(userId, department, RedisRankingPeriod.DAY, yesterday, today, todayStartMs);
                // 주간 key는 월요일 자정에만 바뀐다.
                if (today.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                    rollPeriod(userId, department, RedisRankingPeriod.WEEK, yesterday, today, todayStartMs);
                }
                // 월간 key는 매월 1일 자정에만 바뀐다.
                if (today.getDayOfMonth() == 1) {
                    rollPeriod(userId, department, RedisRankingPeriod.MONTH, yesterday, today, todayStartMs);
                }
            }
        } catch (Exception e) {
            log.warn("[REDIS_RANKING] Failed to roll active sessions to current period.", e);
        }
    }

    /**
     * 한 사용자를 done ZSET에서 active ZSET으로 이동한다.
     *
     * 공부 중 ZSET에 저장하는 점수는 화면에 보여줄 실제 점수가 아니다.
     * 조회기가 현재 시각(epoch ms)을 더했을 때 실제 점수가 되는 보정 점수다.
     */
    private void moveToActive(String doneKey, String activeKey, String userId, long contributionStartMs, Duration ttl) {
        if (redisTemplate.opsForZSet().score(activeKey, userId) != null) {
            // 이미 active에 있으면 중복 시작 이벤트로 보고 idempotent하게 무시한다.
            return;
        }

        // 기존 완료 점수를 읽고 done key에서는 제거한다.
        double baseScore = score(doneKey, userId);
        redisTemplate.opsForZSet().remove(doneKey, userId);
        // active key에는 현재 시각을 더했을 때 실제 점수가 되는 보정 점수를 저장한다.
        redisTemplate.opsForZSet().add(activeKey, userId, baseScore - contributionStartMs);
        // 새로 생성되거나 갱신된 key에 기간별 TTL을 부여한다.
        redisTemplate.expire(doneKey, ttl);
        redisTemplate.expire(activeKey, ttl);
    }

    /**
     * 종료된 세션을 영향을 받는 각 기간에 반영한다.
     *
     * 예시: 일요일 23:50부터 월요일 00:10까지 공부한 경우
     * - 지난 주 세그먼트는 월요일 00:00까지 반영한다.
     * - 이번 주 세그먼트는 월요일 00:00부터 반영한다.
     */
    private void applyFinishedSegments(
            RedisRankingSessionSnapshot session,
            RedisRankingPeriod period,
            String userId,
            Department department
    ) {
        LocalDateTime cursor = session.startTime();
        while (cursor.isBefore(session.endTime())) {
            // cursor가 속한 기간의 끝을 구해서 이번 루프에서 반영할 세그먼트 끝을 결정한다.
            LocalDate cursorDate = cursor.toLocalDate();
            LocalDateTime periodEnd = period.nextStart(cursorDate);
            LocalDateTime segmentEnd = min(periodEnd, session.endTime());
            String periodId = period.id(cursorDate);
            long segmentEndMs = toEpochMs(segmentEnd);
            // active key가 없는 보조 경로에서는 세그먼트 길이만큼 increment한다.
            long segmentMillis = Math.max(0L, segmentEndMs - toEpochMs(cursor));

            // department가 null이면 개인 key, 값이 있으면 학과 key에 반영한다.
            String doneKey = department == null
                    ? RedisRankingKeys.userDone(period, periodId)
                    : RedisRankingKeys.departmentDone(department, period, periodId);
            String activeKey = department == null
                    ? RedisRankingKeys.userActive(period, periodId)
                    : RedisRankingKeys.departmentActive(department, period, periodId);

            Double activeScore = redisTemplate.opsForZSet().score(activeKey, userId);
            if (activeScore != null) {
                // 일반 경로: active 보정 점수 + 세그먼트 종료 시각 = 확정된 실제 점수.
                redisTemplate.opsForZSet().remove(activeKey, userId);
                redisTemplate.opsForZSet().add(doneKey, userId, activeScore + segmentEndMs);
            } else if (segmentMillis > 0) {
                // 경계 처리 후 해당 active key에 사용자가 없을 때는 세그먼트 시간만 누적한다.
                redisTemplate.opsForZSet().incrementScore(doneKey, userId, segmentMillis);
            }
            redisTemplate.expire(doneKey, ttl(period));
            redisTemplate.expire(activeKey, ttl(period));

            // 다음 세그먼트는 현재 세그먼트 종료 시각부터 시작한다.
            cursor = segmentEnd;
        }
    }

    /**
     * 한 활성 사용자를 개인 key와 학과 key 모두에서 다음 기간으로 넘긴다.
     */
    private void rollPeriod(
            Long userId,
            Department department,
            RedisRankingPeriod period,
            LocalDate previousDate,
            LocalDate currentDate,
            long boundaryMs
    ) {
        String userIdValue = userId.toString();
        // 개인 랭킹 key를 이전 기간 active에서 현재 기간 active로 넘긴다.
        rollOneKey(
                RedisRankingKeys.userDone(period, period.id(previousDate)),
                RedisRankingKeys.userActive(period, period.id(previousDate)),
                RedisRankingKeys.userDone(period, period.id(currentDate)),
                RedisRankingKeys.userActive(period, period.id(currentDate)),
                userIdValue,
                boundaryMs,
                ttl(period)
        );
        // 학과 랭킹 key도 같은 방식으로 기간 경계를 넘긴다.
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

    /**
     * 이전 기간 active key를 done으로 확정하고, 현재 기간 active key를 경계 시각부터 다시 시작한다.
     */
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
            // 이미 처리됐거나 해당 기간 active key에 없으면 중복 처리하지 않는다.
            return;
        }

        // 이전 기간에는 경계 시각까지의 점수를 확정한다.
        redisTemplate.opsForZSet().remove(previousActiveKey, userId);
        redisTemplate.opsForZSet().add(previousDoneKey, userId, previousActiveScore + boundaryMs);

        // 현재 기간은 경계 시각부터 active 점수가 증가하도록 시작한다.
        double currentBase = score(currentDoneKey, userId);
        redisTemplate.opsForZSet().remove(currentDoneKey, userId);
        redisTemplate.opsForZSet().add(currentActiveKey, userId, currentBase - boundaryMs);

        redisTemplate.expire(previousDoneKey, ttl);
        redisTemplate.expire(previousActiveKey, ttl);
        redisTemplate.expire(currentDoneKey, ttl);
        redisTemplate.expire(currentActiveKey, ttl);
    }

    /**
     * 기간 경계 처리에 필요한 학과 정보를 활성 세션 메타데이터에서 읽는다.
     */
    private Department activeDepartment(Long userId) {
        Object value = redisTemplate.opsForHash().get(RedisRankingKeys.activeSession(userId), FIELD_DEPARTMENT);
        if (value == null) {
            // 메타데이터가 유실된 사용자는 rollover 대상에서 제외하고, 필요하면 rebuild로 복구한다.
            return null;
        }
        try {
            // Redis hash에는 enum name 문자열이 저장되어 있다.
            return Department.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            // 알 수 없는 학과 값은 무시한다.
            return null;
        }
    }

    private boolean hasRequiredStartData(RedisRankingSessionSnapshot session) {
        // 시작 이벤트에는 세션 id, 사용자 id, 학과, 시작 시간이 반드시 필요하다.
        return session != null
                && session.sessionId() != null
                && session.userId() != null
                && session.department() != null
                && session.startTime() != null;
    }

    private boolean hasRequiredEndData(RedisRankingSessionSnapshot session) {
        // 종료 이벤트는 시작 이벤트 필수 값에 더해 종료 시간과 총 공부 시간이 필요하다.
        return hasRequiredStartData(session)
                && session.endTime() != null
                && session.totalMillis() != null;
    }

    private double score(String key, String userId) {
        // Redis에 점수가 없으면 아직 공부 기록이 없는 사용자로 보고 0점으로 계산한다.
        Double score = redisTemplate.opsForZSet().score(key, userId);
        return score == null ? 0D : score;
    }

    private Duration ttl(RedisRankingPeriod period) {
        // 기간별 랭킹 보관 기간을 분리한다. 과거 조회 가능 기간과 메모리 사용량 사이의 절충이다.
        return switch (period) {
            case DAY -> RANKING_DAY_TTL;
            case WEEK -> RANKING_WEEK_TTL;
            case MONTH -> RANKING_MONTH_TTL;
        };
    }

    private long toEpochMs(LocalDateTime time) {
        // LocalDateTime은 타임존 정보가 없으므로 서버 기본 타임존으로 epoch ms로 변환한다.
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        // 세그먼트 종료 시각은 기간 끝과 세션 종료 시각 중 더 이른 값이다.
        return a.isBefore(b) ? a : b;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            // Redis set member는 문자열로 저장되므로 Long으로 변환한다.
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            // 깨진 member는 무시한다.
            return null;
        }
    }
}
