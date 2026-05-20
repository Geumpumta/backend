package com.gpt.geumpumtabackend.integration.rank.repository;

import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationJob;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationJobStatus;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationTargetType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.domain.UserRanking;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RankingAggregationJobRepository integration")
class RankingAggregationJobRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RankingAggregationJobRepository jobRepository;

    @Autowired
    private UserRankingRepository userRankingRepository;

    @Autowired
    private DepartmentRankingRepository departmentRankingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("same period job is blocked by DB unique constraint")
    void duplicatePeriodJob_throwsDataIntegrityViolation() {
        LocalDateTime start = LocalDateTime.of(2099, 1, 1, 0, 0);
        LocalDateTime end = start.plusDays(1);

        jobRepository.saveAndFlush(RankingAggregationJob.create(
                RankingType.DAILY,
                RankingAggregationTargetType.PERSONAL,
                start,
                end
        ));

        assertThatThrownBy(() -> jobRepository.saveAndFlush(RankingAggregationJob.create(
                RankingType.DAILY,
                RankingAggregationTargetType.PERSONAL,
                start,
                end
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("pending job can be claimed once only")
    void pendingJob_claimedOnce() {
        LocalDateTime now = LocalDateTime.of(2099, 1, 2, 0, 0);
        RankingAggregationJob job = saveJob(RankingType.DAILY, RankingAggregationTargetType.PERSONAL);

        int firstClaim = jobRepository.claim(
                job.getId(),
                "worker-1",
                now.plusMinutes(10),
                now
        );
        int secondClaim = jobRepository.claim(
                job.getId(),
                "worker-2",
                now.plusMinutes(10),
                now
        );

        entityManager.clear();
        RankingAggregationJob reloaded = jobRepository.findById(job.getId()).orElseThrow();

        assertThat(firstClaim).isEqualTo(1);
        assertThat(secondClaim).isZero();
        assertThat(reloaded.getStatus()).isEqualTo(RankingAggregationJobStatus.RUNNING);
        assertThat(reloaded.getLockedBy()).isEqualTo("worker-1");
        assertThat(reloaded.getLockedUntil()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    @DisplayName("failed job can be claimed only after nextRetryAt")
    void failedJob_claimDependsOnRetryTime() {
        LocalDateTime now = LocalDateTime.of(2099, 1, 2, 0, 0);
        RankingAggregationJob retryReady = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.PERSONAL,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        retryReady.markFailed("temporary failure", now.minusMinutes(1), now.minusMinutes(5));
        jobRepository.saveAndFlush(retryReady);

        RankingAggregationJob retryWaiting = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.DEPARTMENT,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        retryWaiting.markFailed("temporary failure", now.plusMinutes(5), now.minusMinutes(5));
        jobRepository.saveAndFlush(retryWaiting);

        int readyClaim = jobRepository.claim(
                retryReady.getId(),
                "worker-1",
                now.plusMinutes(10),
                now
        );
        int waitingClaim = jobRepository.claim(
                retryWaiting.getId(),
                "worker-1",
                now.plusMinutes(10),
                now
        );

        assertThat(readyClaim).isEqualTo(1);
        assertThat(waitingClaim).isZero();
    }

    @Test
    @DisplayName("expired running job is converted to failed")
    void expiredRunningJob_markedFailed() {
        LocalDateTime now = LocalDateTime.of(2099, 1, 2, 0, 0);
        RankingAggregationJob expired = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.PERSONAL,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        expired.markRunning("worker-1", now.minusMinutes(1), now.minusMinutes(10));
        jobRepository.saveAndFlush(expired);

        RankingAggregationJob active = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.DEPARTMENT,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        active.markRunning("worker-2", now.plusMinutes(10), now.minusMinutes(1));
        jobRepository.saveAndFlush(active);

        int updated = jobRepository.expireRunningJobs(now, "RUNNING lock expired");

        entityManager.clear();
        RankingAggregationJob expiredReloaded = jobRepository.findById(expired.getId()).orElseThrow();
        RankingAggregationJob activeReloaded = jobRepository.findById(active.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(expiredReloaded.getStatus()).isEqualTo(RankingAggregationJobStatus.FAILED);
        assertThat(expiredReloaded.getAttemptCount()).isEqualTo(1);
        assertThat(expiredReloaded.getLastError()).isEqualTo("RUNNING lock expired");
        assertThat(activeReloaded.getStatus()).isEqualTo(RankingAggregationJobStatus.RUNNING);
    }

    @Test
    @DisplayName("claim candidates include pending and retry-ready failed jobs only")
    void findClaimCandidates_filtersByStatusRetryAndAttempts() {
        LocalDateTime now = LocalDateTime.of(2099, 1, 10, 0, 0);
        RankingAggregationJob pending = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.PERSONAL,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );

        RankingAggregationJob retryReady = saveJob(
                RankingType.DAILY,
                RankingAggregationTargetType.DEPARTMENT,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        retryReady.markFailed("retry", now.minusMinutes(1), now.minusMinutes(5));
        jobRepository.saveAndFlush(retryReady);

        RankingAggregationJob retryWaiting = saveJob(
                RankingType.WEEKLY,
                RankingAggregationTargetType.PERSONAL,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        retryWaiting.markFailed("retry", now.plusMinutes(5), now.minusMinutes(5));
        jobRepository.saveAndFlush(retryWaiting);

        RankingAggregationJob running = saveJob(
                RankingType.WEEKLY,
                RankingAggregationTargetType.DEPARTMENT,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        running.markRunning("worker", now.plusMinutes(10), now.minusMinutes(1));
        jobRepository.saveAndFlush(running);

        RankingAggregationJob maxAttempts = saveJob(
                RankingType.MONTHLY,
                RankingAggregationTargetType.PERSONAL,
                LocalDateTime.of(2099, 1, 1, 0, 0)
        );
        for (int i = 0; i < 5; i++) {
            maxAttempts.markFailed("retry", now.minusMinutes(1), now.minusMinutes(5));
        }
        jobRepository.saveAndFlush(maxAttempts);

        List<Long> candidates = jobRepository.findClaimCandidates(now, PageRequest.of(0, 10));

        assertThat(candidates).containsExactly(pending.getId(), retryReady.getId());
    }

    @Test
    @DisplayName("same user ranking period row is blocked by DB unique constraint")
    void duplicateUserRanking_throwsDataIntegrityViolation() {
        LocalDateTime calculatedAt = LocalDateTime.of(2099, 1, 1, 0, 0);
        User user = userRepository.saveAndFlush(createUser("unique-user-ranking@test.local"));

        userRankingRepository.saveAndFlush(UserRanking.builder()
                .user(user)
                .rank(1L)
                .totalMillis(1000L)
                .rankingType(RankingType.DAILY)
                .calculatedAt(calculatedAt)
                .build());

        assertThatThrownBy(() -> userRankingRepository.saveAndFlush(UserRanking.builder()
                .user(user)
                .rank(2L)
                .totalMillis(2000L)
                .rankingType(RankingType.DAILY)
                .calculatedAt(calculatedAt)
                .build()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("same department ranking period row is blocked by DB unique constraint")
    void duplicateDepartmentRanking_throwsDataIntegrityViolation() {
        LocalDateTime calculatedAt = LocalDateTime.of(2099, 1, 1, 0, 0);

        departmentRankingRepository.saveAndFlush(DepartmentRanking.builder()
                .department(Department.SOFTWARE)
                .rank(1L)
                .totalMillis(1000L)
                .rankingType(RankingType.DAILY)
                .calculatedAt(calculatedAt)
                .build());

        assertThatThrownBy(() -> departmentRankingRepository.saveAndFlush(DepartmentRanking.builder()
                .department(Department.SOFTWARE)
                .rank(2L)
                .totalMillis(2000L)
                .rankingType(RankingType.DAILY)
                .calculatedAt(calculatedAt)
                .build()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private RankingAggregationJob saveJob(
            RankingType rankingType,
            RankingAggregationTargetType targetType
    ) {
        return saveJob(rankingType, targetType, LocalDateTime.of(2099, 1, 1, 0, 0));
    }

    private RankingAggregationJob saveJob(
            RankingType rankingType,
            RankingAggregationTargetType targetType,
            LocalDateTime periodStart
    ) {
        return jobRepository.saveAndFlush(RankingAggregationJob.create(
                rankingType,
                targetType,
                periodStart,
                periodStart.plusDays(1)
        ));
    }

    private User createUser(String email) {
        return User.builder()
                .email(email)
                .role(UserRole.USER)
                .name("ranking integration user")
                .picture("image.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .providerId(email)
                .department(Department.SOFTWARE)
                .build();
    }
}
