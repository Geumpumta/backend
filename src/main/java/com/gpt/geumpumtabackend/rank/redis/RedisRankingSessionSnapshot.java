package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;

import java.time.LocalDateTime;

public record RedisRankingSessionSnapshot(
        Long sessionId,
        Long userId,
        Department department,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalMillis
) {
    public static RedisRankingSessionSnapshot started(
            Long sessionId,
            Long userId,
            Department department,
            LocalDateTime startTime
    ) {
        return new RedisRankingSessionSnapshot(sessionId, userId, department, startTime, null, null);
    }

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
