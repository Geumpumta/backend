package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.rank.domain.RankingAggregationJob;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationTargetType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RankingAggregationJobCreator {

    private final RankingAggregationJobRepository jobRepository;

    public void createIfAbsent(
            RankingType rankingType,
            RankingAggregationTargetType targetType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        if (jobRepository.existsByRankingTypeAndTargetTypeAndPeriodStartAndPeriodEnd(
                rankingType,
                targetType,
                periodStart,
                periodEnd
        )) {
            return;
        }

        try {
            jobRepository.saveAndFlush(RankingAggregationJob.create(
                    rankingType,
                    targetType,
                    periodStart,
                    periodEnd
            ));
        } catch (DataIntegrityViolationException ignored) {
            // Another scheduler instance created the same job first.
        }
    }

    public void createPair(RankingType rankingType, LocalDateTime periodStart, LocalDateTime periodEnd) {
        createIfAbsent(rankingType, RankingAggregationTargetType.PERSONAL, periodStart, periodEnd);
        createIfAbsent(rankingType, RankingAggregationTargetType.DEPARTMENT, periodStart, periodEnd);
    }
}
