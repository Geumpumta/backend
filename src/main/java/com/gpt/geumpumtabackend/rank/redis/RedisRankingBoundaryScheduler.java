package com.gpt.geumpumtabackend.rank.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRankingBoundaryScheduler {

    private final RedisRealtimeRankingWriter writer;

    /**
     * 공부 중인 사용자는 날짜/주/月 경계에서 새 기간 key로 이동해야 한다.
     *
     * 예시:
     * - 사용자가 23:50에 공부를 시작했고 00:00에도 공부 중이다.
     * - 어제 active ZSET은 00:00까지의 점수로 done ZSET에 확정한다.
     * - 오늘 active ZSET은 00:00부터 다시 시작하도록 만든다.
     *
     * 이 스케줄러는 점수를 1초마다 갱신하지 않는다.
     * 오직 기간 경계에서 사용자가 어느 Redis key에 있어야 하는지만 보정한다.
     */
    @Scheduled(cron = "10 0 0 * * *")
    public void rollActiveSessionsAtPeriodBoundary() {
        writer.rollActiveSessionsToCurrentPeriods();
    }
}
