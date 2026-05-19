package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;

import java.time.LocalDateTime;

/**
 * DB 커밋 이후 StudySessionService에서 Redis 랭킹 쓰기 처리로 넘기는 불변 스냅샷.
 *
 * Redis 업데이트는 트랜잭션 커밋 이후 실행되므로 JPA 엔티티를 그대로 넘기면
 * 지연 로딩 상태에 따라 안전하지 않을 수 있다.
 * 그래서 Redis 쓰기 처리에는 필요한 값만 복사한 스냅샷을 전달한다.
 */
public record RedisRankingSessionSnapshot(
        Long sessionId,
        Long userId,
        Department department,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalMillis
) {
    /**
     * 공부 시작 이벤트용 스냅샷.
     * endTime과 totalMillis는 세션 종료 전까지 알 수 없으므로 null로 둔다.
     */
    public static RedisRankingSessionSnapshot started(
            Long sessionId,
            Long userId,
            Department department,
            LocalDateTime startTime
    ) {
        return new RedisRankingSessionSnapshot(sessionId, userId, department, startTime, null, null);
    }

    /**
     * 공부 종료 이벤트용 스냅샷.
     * totalMillis는 MySQL에 저장된 값을 Redis 조회 모델에 반영하기 위해 사용한다.
     */
    public static RedisRankingSessionSnapshot ended(
            Long sessionId,
            Long userId,
            Department department,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long totalMillis
    ) {
        return new RedisRankingSessionSnapshot(sessionId, userId, department, startTime, endTime, totalMillis);
    }
}
