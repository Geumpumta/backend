package com.gpt.geumpumtabackend.rank.scheduler;

import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RankingAggregationExpiredJobScanner {

    private final RankingAggregationJobRepository jobRepository;

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "ranking-expired-running-job-scanner", lockAtMostFor = "PT2M")
    @Transactional
    public void expireRunningJobs() {
        jobRepository.expireRunningJobs(
                LocalDateTime.now(),
                "RUNNING lock expired"
        );
    }
}
