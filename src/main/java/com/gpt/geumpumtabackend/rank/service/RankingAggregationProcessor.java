package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationJob;
import com.gpt.geumpumtabackend.rank.domain.RankingAggregationTargetType;
import com.gpt.geumpumtabackend.rank.domain.UserRanking;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.repository.RankingAggregationJobRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingAggregationProcessor {

    private final RankingAggregationJobRepository jobRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final UserRankingRepository userRankingRepository;
    private final DepartmentRankingRepository departmentRankingRepository;

    @Transactional
    public void process(Long jobId) {
        RankingAggregationJob job = jobRepository.findById(jobId).orElseThrow();

        if (job.getTargetType() == RankingAggregationTargetType.PERSONAL) {
            processPersonal(job);
        } else {
            processDepartment(job);
        }

        job.markSuccess(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDataIntegrityFailureInNewTransaction(Long jobId, Exception exception) {
        RankingAggregationJob job = jobRepository.findById(jobId).orElseThrow();

        if (isAlreadyComplete(job)) {
            job.markSuccess(LocalDateTime.now());
            return;
        }

        markFailed(job, exception);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTransaction(Long jobId, Exception exception) {
        RankingAggregationJob job = jobRepository.findById(jobId).orElseThrow();
        markFailed(job, exception);
    }

    private void markFailed(RankingAggregationJob job, Exception exception) {
        LocalDateTime now = LocalDateTime.now();
        int nextAttemptCount = job.getAttemptCount() + 1;
        job.markFailed(
                exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                now.plusMinutes(backoffMinutes(nextAttemptCount)),
                now
        );
    }

    private void processPersonal(RankingAggregationJob job) {
        List<PersonalRankingTemp> rows = studySessionRepository.calculateFinalizedPeriodRanking(
                job.getPeriodStart(),
                job.getPeriodEnd()
        );

        Map<Long, User> users = userRepository.findAllById(
                        rows.stream().map(PersonalRankingTemp::getUserId).toList()
                ).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<UserRanking> rankings = rows.stream()
                .map(row -> {
                    User user = users.get(row.getUserId());
                    if (user == null) {
                        throw new BusinessException(ExceptionType.USER_NOT_FOUND);
                    }
                    return UserRanking.builder()
                            .user(user)
                            .rank(row.getRanking())
                            .totalMillis(row.getTotalMillis())
                            .rankingType(job.getRankingType())
                            .calculatedAt(job.getPeriodStart())
                            .build();
                })
                .toList();

        userRankingRepository.saveAll(rankings);
        userRankingRepository.flush();
    }

    private void processDepartment(RankingAggregationJob job) {
        List<DepartmentRankingTemp> rows = studySessionRepository.calculateFinalizedDepartmentRanking(
                job.getPeriodStart(),
                job.getPeriodEnd()
        );

        List<DepartmentRanking> rankings = rows.stream()
                .map(row -> DepartmentRanking.builder()
                        .department(Department.valueOf(row.getDepartment()))
                        .rank(row.getRanking())
                        .totalMillis(row.getTotalMillis())
                        .rankingType(job.getRankingType())
                        .calculatedAt(job.getPeriodStart())
                        .build())
                .toList();

        departmentRankingRepository.saveAll(rankings);
        departmentRankingRepository.flush();
    }

    private boolean isAlreadyComplete(RankingAggregationJob job) {
        if (job.getTargetType() == RankingAggregationTargetType.PERSONAL) {
            long expected = studySessionRepository.calculateFinalizedPeriodRanking(
                    job.getPeriodStart(),
                    job.getPeriodEnd()
            ).size();
            long actual = userRankingRepository.countByRankingTypeAndCalculatedAt(
                    job.getRankingType(),
                    job.getPeriodStart()
            );
            return expected == actual;
        }

        long expected = studySessionRepository.calculateFinalizedDepartmentRanking(
                job.getPeriodStart(),
                job.getPeriodEnd()
        ).size();
        long actual = departmentRankingRepository.countByRankingTypeAndCalculatedAt(
                job.getRankingType(),
                job.getPeriodStart()
        );
        return expected == actual;
    }

    private long backoffMinutes(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            default -> 60;
        };
    }
}
