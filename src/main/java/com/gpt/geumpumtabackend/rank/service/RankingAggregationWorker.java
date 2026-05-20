package com.gpt.geumpumtabackend.rank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingAggregationWorker {

    private final RankingAggregationJobClaimService claimService;
    private final RankingAggregationProcessor processor;

    public void tryProcess(Long jobId, String workerId) {
        if (!claimService.claim(jobId, workerId)) {
            return;
        }

        try {
            processor.process(jobId);
        } catch (DataIntegrityViolationException e) {
            log.error("[RANKING_AGGREGATION_JOB_DUPLICATED] jobId={}", jobId, e);
            processor.handleDataIntegrityFailureInNewTransaction(jobId, e);
        } catch (Exception e) {
            log.error("[RANKING_AGGREGATION_JOB_FAILED] jobId={}", jobId, e);
            processor.markFailedInNewTransaction(jobId, e);
        }
    }
}
