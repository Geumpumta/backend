package com.gpt.geumpumtabackend.rank.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ranking_aggregation_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ranking_aggregation_job_period",
                columnNames = {"ranking_type", "target_type", "period_start", "period_end"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingAggregationJob extends BaseEntity {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int LAST_ERROR_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "ranking_type", length = 20)
    private RankingType rankingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "target_type", length = 20)
    private RankingAggregationTargetType targetType;

    @Column(nullable = false, name = "period_start")
    private LocalDateTime periodStart;

    @Column(nullable = false, name = "period_end")
    private LocalDateTime periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingAggregationJobStatus status;

    @Column(nullable = false, name = "attempt_count")
    private int attemptCount;

    @Column(nullable = false, name = "max_attempts")
    private int maxAttempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public static RankingAggregationJob create(
            RankingType rankingType,
            RankingAggregationTargetType targetType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        RankingAggregationJob job = new RankingAggregationJob();
        job.rankingType = rankingType;
        job.targetType = targetType;
        job.periodStart = periodStart;
        job.periodEnd = periodEnd;
        job.status = RankingAggregationJobStatus.PENDING;
        job.attemptCount = 0;
        job.maxAttempts = DEFAULT_MAX_ATTEMPTS;
        return job;
    }

    public void markSuccess(LocalDateTime now) {
        this.status = RankingAggregationJobStatus.SUCCESS;
        this.lockedBy = null;
        this.lockedUntil = null;
        this.nextRetryAt = null;
        this.finishedAt = now;
        this.lastError = null;
    }

    public void markRunning(String lockedBy, LocalDateTime lockedUntil, LocalDateTime now) {
        this.status = RankingAggregationJobStatus.RUNNING;
        this.lockedBy = lockedBy;
        this.lockedUntil = lockedUntil;
        this.startedAt = now;
    }

    public void markFailed(String errorMessage, LocalDateTime nextRetryAt, LocalDateTime now) {
        this.status = RankingAggregationJobStatus.FAILED;
        this.attemptCount++;
        this.nextRetryAt = nextRetryAt;
        this.lockedBy = null;
        this.lockedUntil = null;
        this.finishedAt = now;
        this.lastError = truncate(errorMessage);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= LAST_ERROR_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
