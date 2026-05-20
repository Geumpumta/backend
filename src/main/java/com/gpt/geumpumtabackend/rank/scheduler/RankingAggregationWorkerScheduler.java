package com.gpt.geumpumtabackend.rank.scheduler;

import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import com.gpt.geumpumtabackend.rank.service.RankingAggregationWorker;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingAggregationWorkerScheduler {

    private static final int BATCH_SIZE = 10;

    private final RankingAggregationJobRepository jobRepository;
    private final RankingAggregationWorker worker;
    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelay = 30_000)
    @SchedulerLock(name = "ranking-aggregation-worker", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
    public void run() {
        List<Long> jobIds = jobRepository.findClaimCandidates(
                LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)
        );

        for (Long jobId : jobIds) {
            worker.tryProcess(jobId, workerId);
        }
    }
}
