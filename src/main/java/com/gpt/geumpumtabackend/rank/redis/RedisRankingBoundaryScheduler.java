package com.gpt.geumpumtabackend.rank.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRankingBoundaryScheduler {

    private final RedisRealtimeRankingWriter writer;

    @Scheduled(cron = "10 0 0 * * *")
    public void rollActiveSessionsAtPeriodBoundary() {
        writer.rollActiveSessionsToCurrentPeriods();
    }
}
