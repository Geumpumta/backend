package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RankingAggregationJobClaimService {

    private static final long LOCK_MINUTES = 10;

    private final RankingAggregationJobRepository jobRepository;

    @Transactional
    public boolean claim(Long jobId, String workerId) {
        LocalDateTime now = LocalDateTime.now();
        int updated = jobRepository.claim(jobId, workerId, now.plusMinutes(LOCK_MINUTES), now);
        return updated == 1;
    }
}
