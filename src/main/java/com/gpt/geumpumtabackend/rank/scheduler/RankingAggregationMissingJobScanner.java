package com.gpt.geumpumtabackend.rank.scheduler;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.service.RankingAggregationJobCreator;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RankingAggregationMissingJobScanner {

    private static final int DAILY_SCAN_DAYS = 14;
    private static final int WEEKLY_SCAN_WEEKS = 8;
    private static final int MONTHLY_SCAN_MONTHS = 6;

    private final RankingAggregationJobCreator jobCreator;

    @Scheduled(cron = "0 10 0 * * *")
    @SchedulerLock(name = "ranking-missing-job-scanner", lockAtMostFor = "PT10M")
    public void scan() {
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= DAILY_SCAN_DAYS; i++) {
            LocalDate day = today.minusDays(i);
            jobCreator.createPair(
                    RankingType.DAILY,
                    day.atStartOfDay(),
                    day.plusDays(1).atStartOfDay()
            );
        }

        for (int i = 1; i <= WEEKLY_SCAN_WEEKS; i++) {
            LocalDate weekStart = today.minusWeeks(i).with(DayOfWeek.MONDAY);
            jobCreator.createPair(
                    RankingType.WEEKLY,
                    weekStart.atStartOfDay(),
                    weekStart.plusWeeks(1).atStartOfDay()
            );
        }

        for (int i = 1; i <= MONTHLY_SCAN_MONTHS; i++) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            jobCreator.createPair(
                    RankingType.MONTHLY,
                    monthStart.atStartOfDay(),
                    monthStart.plusMonths(1).atStartOfDay()
            );
        }
    }
}
