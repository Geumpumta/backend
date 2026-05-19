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
/**
 * Redis 실시간 랭킹 조회 모델 복구 서비스.
 *
 * Redis는 원본 저장소가 아니므로 장애, flush, 배포 중 누락이 발생하면 MySQL 기준으로 다시 만들 수 있어야 한다.
 * 이 서비스는 현재 일간/주간/월간 구간의 완료 세션과 진행 중 세션을 MySQL에서 읽어 Redis 키를 재구성한다.
 *
 * 현재는 내부 서비스 메서드만 제공한다.
 * 운영에서는 관리자 API, 배치, 수동 실행 커맨드 등에서 rebuildCurrentPeriods()를 호출하면 된다.
 */
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

    /**
     * 현재 날짜 기준 일간/주간/월간 Redis 랭킹 조회 모델을 전체 재생성한다.
     *
     * 처리 순서:
     * 1. 현재 기간 Redis 랭킹 key를 삭제한다.
     * 2. MySQL의 종료된 study_session을 기간별 done ZSET에 누적한다.
     * 3. MySQL의 STARTED study_session을 active ZSET에 넣는다.
     * 4. active session HASH와 active users SET을 다시 만든다.
     */
    public void rebuildCurrentPeriods() {
        LocalDate today = LocalDate.now();
        // 현재 날짜에 해당하는 일/주/月 Redis key를 먼저 비워 중복 누적을 방지한다.
        clearCurrentRankingKeys(today);

        for (RedisRankingPeriod period : RedisRankingPeriod.values()) {
            // 각 기간별로 완료 세션과 진행 중 세션을 MySQL에서 다시 읽어 채운다.
            rebuildPeriod(period, today);
        }

        // 종료 이벤트 검증과 기간 경계 처리에 필요한 active metadata를 별도로 복구한다.
        rebuildActiveSessionMetadata();
        log.info("[REDIS_RANKING] Rebuilt current Redis ranking read model.");
    }

    private void rebuildPeriod(RedisRankingPeriod period, LocalDate date) {
        // 복구할 기간의 시작, 끝, Redis period id를 같은 기준으로 계산한다.
        LocalDateTime periodStart = period.start(date);
        LocalDateTime periodEnd = period.nextStart(date);
        String periodId = period.id(date);

        // 이미 종료된 세션은 done ZSET에 확정 점수로 누적한다.
        for (UserScore score : completedScores(periodStart, periodEnd)) {
            addDoneScore(period, periodId, score.userId(), score.department(), score.totalMillis());
        }

        // 현재 진행 중인 세션은 done에서 active로 이동한 형태로 재구성한다.
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
            // 0점 이하는 랭킹에 저장하지 않아 Redis key 크기를 줄인다.
            return;
        }

        // 개인 랭킹과 학과 랭킹을 모두 같은 완료 점수로 채운다.
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
        // 세션이 기간 시작 전부터 진행 중이었다면 현재 기간 기여분은 기간 시작 시각부터 계산한다.
        long contributionStartMs = toEpochMs(max(activeSession.startTime(), period.start(LocalDate.now())));

        // 개인 랭킹을 active 보정 점수 형태로 복구한다.
        moveOneKeyToActive(
                RedisRankingKeys.userDone(period, periodId),
                RedisRankingKeys.userActive(period, periodId),
                member,
                contributionStartMs
        );
        // 학과 랭킹도 같은 active 보정 점수 형태로 복구한다.
        moveOneKeyToActive(
                RedisRankingKeys.departmentDone(activeSession.department(), period, periodId),
                RedisRankingKeys.departmentActive(activeSession.department(), period, periodId),
                member,
                contributionStartMs
        );
        expirePeriodKeys(period, periodId, activeSession.department());
    }

    private void moveOneKeyToActive(String doneKey, String activeKey, String member, long contributionStartMs) {
        // 같은 사용자의 완료 점수가 이미 done에 있다면 baseScore로 보존한다.
        double baseScore = score(doneKey, member);
        redisTemplate.opsForZSet().remove(doneKey, member);
        // active 점수는 조회기가 현재 시각을 더했을 때 현재 점수가 되도록 보정 점수로 저장한다.
        redisTemplate.opsForZSet().add(activeKey, member, baseScore - contributionStartMs);
    }

    /**
     * Redis 종료 이벤트 검증과 기간 경계 처리에 필요한 활성 세션 메타데이터를 다시 만든다.
     */
    private void rebuildActiveSessionMetadata() {
        List<ActiveSession> activeSessions = activeSessions(LocalDate.now().atStartOfDay(), RedisRankingPeriod.MONTH.nextStart(LocalDate.now()));
        for (ActiveSession activeSession : activeSessions) {
            String member = activeSession.userId().toString();
            // 세션 종료 시 sessionId 검증에 사용할 hash 데이터를 다시 만든다.
            Map<String, String> metadata = new HashMap<>();
            metadata.put(FIELD_SESSION_ID, activeSession.sessionId().toString());
            metadata.put(FIELD_START_AT_MS, Long.toString(toEpochMs(activeSession.startTime())));
            metadata.put(FIELD_DEPARTMENT, activeSession.department().name());

            redisTemplate.opsForHash().putAll(RedisRankingKeys.activeSession(activeSession.userId()), metadata);
            redisTemplate.expire(RedisRankingKeys.activeSession(activeSession.userId()), ACTIVE_SESSION_TTL);
            // 자정 rollover 대상이 되도록 active user set에도 다시 넣는다.
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

        // 기간과 겹치는 종료 세션만 읽고, 실제 기간 안에 들어온 시간만 ms 단위로 합산한다.
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Department department = parseDepartment(rs.getString("department"));
            if (department == null) {
                // 학과 값이 잘못된 사용자는 학과 랭킹을 만들 수 없으므로 제외한다.
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

        // 현재 STARTED 상태인 세션을 읽어 active ZSET 복구 대상으로 사용한다.
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Department department = parseDepartment(rs.getString("department"));
            if (department == null) {
                // 학과 값이 잘못된 활성 세션은 학과 key를 계산할 수 없으므로 제외한다.
                return null;
            }
            Timestamp startTime = rs.getTimestamp("start_time");
            if (startTime == null || !startTime.toLocalDateTime().isBefore(periodEnd)) {
                // 기간과 겹치지 않는 세션은 현재 기간 active 랭킹에 넣지 않는다.
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
            // 개인 done/active key를 삭제 대상에 넣는다.
            keys.add(RedisRankingKeys.userDone(period, periodId));
            keys.add(RedisRankingKeys.userActive(period, periodId));
            for (Department department : Department.values()) {
                // 모든 학과의 done/active key도 삭제 대상에 넣는다.
                keys.add(RedisRankingKeys.departmentDone(department, period, periodId));
                keys.add(RedisRankingKeys.departmentActive(department, period, periodId));
            }
        }

        // 기존 active metadata도 삭제하고 MySQL STARTED 세션 기준으로 다시 만든다.
        var activeUsers = redisTemplate.opsForSet().members(RedisRankingKeys.ACTIVE_USERS);
        if (activeUsers != null) {
            for (Object rawUserId : activeUsers) {
                Long userId = parseLong(rawUserId);
                if (userId != null) {
                    // 기존 활성 세션 hash도 삭제 후 MySQL 기준으로 다시 만든다.
                    keys.add(RedisRankingKeys.activeSession(userId));
                }
            }
        }
        keys.add(RedisRankingKeys.ACTIVE_USERS);

        // 모은 key를 한 번에 삭제한다. 없는 key가 포함되어도 Redis delete는 안전하다.
        redisTemplate.delete(keys);
    }

    /**
     * 기간별 key에 TTL을 다시 설정한다.
     */
    private void expirePeriodKeys(RedisRankingPeriod period, String periodId, Department department) {
        Duration ttl = ttl(period);
        // 복구로 새로 만들어진 key에도 운영 writer와 같은 TTL 정책을 적용한다.
        redisTemplate.expire(RedisRankingKeys.userDone(period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.userActive(period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.departmentDone(department, period, periodId), ttl);
        redisTemplate.expire(RedisRankingKeys.departmentActive(department, period, periodId), ttl);
    }

    private double score(String key, String member) {
        // 복구 중 base 점수가 없으면 0점으로 간주한다.
        Double score = redisTemplate.opsForZSet().score(key, member);
        return score == null ? 0D : score;
    }

    private Duration ttl(RedisRankingPeriod period) {
        // 운영 writer와 동일한 기간별 TTL을 사용한다.
        return switch (period) {
            case DAY -> RANKING_DAY_TTL;
            case WEEK -> RANKING_WEEK_TTL;
            case MONTH -> RANKING_MONTH_TTL;
        };
    }

    private long toEpochMs(LocalDateTime time) {
        // DB LocalDateTime을 서버 기본 타임존 기준 epoch ms로 변환한다.
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        // 세션 시작 시각과 기간 시작 시각 중 더 늦은 값을 현재 기간 기여 시작점으로 사용한다.
        return a.isAfter(b) ? a : b;
    }

    private Department parseDepartment(String value) {
        if (value == null || value.isBlank()) {
            // 학과가 비어 있으면 학과 랭킹 복구 대상에서 제외한다.
            return null;
        }
        try {
            // DB에는 Department enum name이 저장되어 있다고 가정한다.
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
            // Redis set member를 Long userId로 변환한다.
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            // 깨진 member는 삭제 목록 구성에서 제외한다.
            return null;
        }
    }

    private record UserScore(Long userId, Department department, Long totalMillis) {
    }

    private record ActiveSession(Long sessionId, Long userId, Department department, LocalDateTime startTime) {
    }
}
