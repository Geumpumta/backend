package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.RankingAggregationJob;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationTargetType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RankingAggregationJobRepository extends JpaRepository<RankingAggregationJob, Long> {

    boolean existsByRankingTypeAndTargetTypeAndPeriodStartAndPeriodEnd(
            RankingType rankingType,
            RankingAggregationTargetType targetType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    @Query("""
            select j.id
            from RankingAggregationJob j
            where j.status in (
                com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.PENDING,
                com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.FAILED
            )
              and j.attemptCount < j.maxAttempts
              and (j.nextRetryAt is null or j.nextRetryAt <= :now)
              and (j.lockedUntil is null or j.lockedUntil < :now)
            order by j.createdAt asc, j.id asc
            """)
    List<Long> findClaimCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RankingAggregationJob j
            set j.status = com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.RUNNING,
                j.lockedBy = :workerId,
                j.lockedUntil = :lockedUntil,
                j.startedAt = :now
            where j.id = :jobId
              and j.status in (
                  com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.PENDING,
                  com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.FAILED
              )
              and j.attemptCount < j.maxAttempts
              and (j.nextRetryAt is null or j.nextRetryAt <= :now)
              and (j.lockedUntil is null or j.lockedUntil < :now)
            """)
    int claim(
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("lockedUntil") LocalDateTime lockedUntil,
            @Param("now") LocalDateTime now
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RankingAggregationJob j
            set j.status = com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.FAILED,
                j.attemptCount = j.attemptCount + 1,
                j.lockedBy = null,
                j.lockedUntil = null,
                j.nextRetryAt = :now,
                j.lastError = :errorMessage,
                j.finishedAt = :now
            where j.status = com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus.RUNNING
              and j.lockedUntil < :now
              and j.attemptCount < j.maxAttempts
            """)
    int expireRunningJobs(
            @Param("now") LocalDateTime now,
            @Param("errorMessage") String errorMessage
    );
}
