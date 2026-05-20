package com.gpt.geumpumtabackend.rank.scheduler;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.service.RankingAggregationJobCreator;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RankingSchedulerService {

    private final RankingAggregationJobCreator jobCreator;

    @Scheduled(cron = "5 0 0 * * *")
    @SchedulerLock(name = "ranking-daily-job-creation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void dailyRankingScheduler() {
        LocalDate day = LocalDate.now().minusDays(1);
        jobCreator.createPair(
                RankingType.DAILY,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay()
        );
    }

    @Scheduled(cron = "0 1 0 ? * MON")
    @SchedulerLock(name = "ranking-weekly-job-creation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void weeklyRankingScheduler() {
        LocalDate start = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        jobCreator.createPair(
                RankingType.WEEKLY,
                start.atStartOfDay(),
                start.plusWeeks(1).atStartOfDay()
        );
    }

    @Scheduled(cron = "0 2 0 1 * ?")
    @SchedulerLock(name = "ranking-monthly-job-creation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void monthlyRankingScheduler() {
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        jobCreator.createPair(
                RankingType.MONTHLY,
                start.atStartOfDay(),
                start.plusMonths(1).atStartOfDay()
        );
    }
}
