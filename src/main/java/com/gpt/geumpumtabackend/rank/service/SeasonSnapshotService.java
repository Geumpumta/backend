package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.*;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SeasonSnapshotService {

    private final UserRankingRepository userRankingRepository;
    private final SeasonRankingSnapshotRepository snapshotRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonSnapshotBatchService batchService;


    @Retryable(
        retryFor = {DataAccessException.class, SQLException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    @Transactional
    public int createSeasonSnapshot(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new BusinessException(ExceptionType.SEASON_NOT_FOUND));

        if (snapshotRepository.existsBySeasonId(seasonId)) {
            return 0;
        }

        LocalDateTime seasonStart = season.getStartDate().atStartOfDay();
        LocalDateTime seasonEndInclusive = season.getEndDate().plusDays(1).atStartOfDay();
        LocalDateTime snapshotAt = LocalDateTime.now();


        List<PersonalRankingTemp> overallRankings = calculateSeasonRanking(
            seasonStart, seasonEndInclusive
        );

        List<SeasonRankingSnapshot> overallSnapshots = overallRankings.stream()
            .map(temp -> SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(temp.getUserId())
                .rankType(RankType.OVERALL)
                .finalRank(temp.getRanking().intValue())
                .finalTotalMillis(temp.getTotalMillis())
                .snapshotAt(snapshotAt)
                .build())
            .collect(Collectors.toList());

        batchService.saveBatchWithJdbc(overallSnapshots);

        int deptCount = 0;
        for (Department dept : Department.values()) {
            List<PersonalRankingTemp> deptRankings = calculateSeasonDepartmentRanking(
                seasonStart, seasonEndInclusive, dept
            );

            if (deptRankings.isEmpty()) {
                continue;
            }

            List<SeasonRankingSnapshot> deptSnapshots = deptRankings.stream()
                .map(temp -> SeasonRankingSnapshot.builder()
                    .seasonId(seasonId)
                    .userId(temp.getUserId())
                    .rankType(RankType.DEPARTMENT)
                    .department(dept)
                    .finalRank(temp.getRanking().intValue())
                    .finalTotalMillis(temp.getTotalMillis())
                    .snapshotAt(snapshotAt)
                    .build())
                .collect(Collectors.toList());

            batchService.saveBatchWithJdbc(deptSnapshots);
            deptCount += deptSnapshots.size();
        }

        int totalCount = overallSnapshots.size() + deptCount;

        return totalCount;
    }


    @Recover
    public int recoverCreateSeasonSnapshot(Exception e, Long seasonId) {
        log.error("[SNAPSHOT_FAILED] Season {} snapshot creation failed after 3 retries",
                  seasonId, e);
        return 0;
    }


    private List<PersonalRankingTemp> calculateSeasonRanking(
            LocalDateTime seasonStart, LocalDateTime seasonEnd) {

        List<PersonalRankingTemp> monthlyData = userRankingRepository
            .calculateSeasonRankingFromMonthlyRankings(seasonStart, seasonEnd);

        return assignRanks(monthlyData);
    }


    private List<PersonalRankingTemp> calculateSeasonDepartmentRanking(
            LocalDateTime seasonStart, LocalDateTime seasonEnd, Department department) {

        List<PersonalRankingTemp> monthlyData = userRankingRepository
            .calculateSeasonDepartmentRankingFromMonthlyRankings(
                seasonStart, seasonEnd, department
            );

        return assignRanks(monthlyData);
    }


    private List<PersonalRankingTemp> assignRanks(List<PersonalRankingTemp> data) {
        List<PersonalRankingTemp> sorted = data.stream()
            .sorted(Comparator.comparing(PersonalRankingTemp::getTotalMillis).reversed())
            .toList();

        List<PersonalRankingTemp> result = new ArrayList<>();
        long currentRank = 1;
        Long previousMillis = null;

        for (int i = 0; i < sorted.size(); i++) {
            PersonalRankingTemp temp = sorted.get(i);

            if (previousMillis == null || !previousMillis.equals(temp.getTotalMillis())) {
                currentRank = i + 1;
            }

            result.add(new PersonalRankingTemp(
                temp.getUserId(),
                temp.getNickname(),
                temp.getImageUrl(),
                temp.getDepartment(),
                temp.getTotalMillis(),
                currentRank
            ));

            previousMillis = temp.getTotalMillis();
        }

        return result;
    }
}
