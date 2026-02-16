package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.*;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.SeasonDepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.SeasonRankingResponse;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SeasonRankService {

    private final UserRankingRepository userRankingRepository;
    private final DepartmentRankingRepository departmentRankingRepository;
    private final StudySessionRepository studySessionRepository;
    private final SeasonService seasonService;
    private final SeasonRepository seasonRepository;
    private final SeasonRankingSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;


    public SeasonRankingResponse getCurrentSeasonRanking() {
        Season activeSeason = seasonService.getActiveSeason();

        LocalDate seasonStart = activeSeason.getStartDate();
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);

        List<PersonalRankingTemp> allData = new ArrayList<>();

        if (currentMonthStart.isAfter(seasonStart)) {
            List<PersonalRankingTemp> completedMonths = userRankingRepository
                .calculateSeasonRankingFromMonthlyRankings(
                    seasonStart.atStartOfDay(),
                    currentMonthStart.atStartOfDay()
                );
            allData.addAll(completedMonths);
        }

        if (today.isAfter(currentMonthStart)) {
            List<PersonalRankingTemp> currentMonth = userRankingRepository
                .calculateCurrentMonthRankingFromDailyRankings(
                    currentMonthStart.atStartOfDay(),
                    today.atStartOfDay()
                );
            allData.addAll(currentMonth);
        }

        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        List<PersonalRankingTemp> todayRanking = studySessionRepository
            .calculateCurrentPeriodRanking(
                today.atStartOfDay(),
                todayEnd,
                LocalDateTime.now()
            );
        allData.addAll(todayRanking);

        List<PersonalRankingTemp> finalRankings = mergeAndRank(allData);

        return SeasonRankingResponse.of(activeSeason, finalRankings);
    }


    public SeasonDepartmentRankingResponse getCurrentSeasonDepartmentRanking(Long userId) {
        Season activeSeason = seasonService.getActiveSeason();

        LocalDate seasonStart = activeSeason.getStartDate();
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);

        List<DepartmentRankingTemp> allData = new ArrayList<>();

        if (currentMonthStart.isAfter(seasonStart)) {
            List<DepartmentRankingTemp> completedMonths = departmentRankingRepository
                .calculateSeasonFromMonthlyDepartmentRankings(
                    seasonStart.atStartOfDay(),
                    currentMonthStart.atStartOfDay()
                );
            allData.addAll(completedMonths);
        }

        if (today.isAfter(currentMonthStart)) {
            List<DepartmentRankingTemp> currentMonth = departmentRankingRepository
                .calculateCurrentMonthFromDailyDepartmentRankings(
                    currentMonthStart.atStartOfDay(),
                    today.atStartOfDay()
                );
            allData.addAll(currentMonth);
        }

        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        List<DepartmentRankingTemp> todayRanking = studySessionRepository
            .calculateCurrentDepartmentRanking(
                today.atStartOfDay(),
                todayEnd,
                LocalDateTime.now()
            );
        allData.addAll(todayRanking);

        List<DepartmentRankingTemp> finalRankings = mergeAndRankDepartments(allData);

        return buildSeasonDepartmentRankingResponse(activeSeason, finalRankings, userId);
    }


    public SeasonRankingResponse getEndedSeasonRanking(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new BusinessException(ExceptionType.SEASON_NOT_FOUND));

        if (season.getStatus() == SeasonStatus.ACTIVE) {
            throw new BusinessException(ExceptionType.SEASON_NOT_ENDED);
        }

        List<SeasonRankingSnapshot> snapshots = snapshotRepository
            .findBySeasonIdAndRankType(seasonId, RankType.OVERALL);

        List<PersonalRankingTemp> rankings = convertSnapshotsToRankings(snapshots);

        return SeasonRankingResponse.of(season, rankings);
    }


    public SeasonDepartmentRankingResponse getEndedSeasonDepartmentRanking(Long seasonId, Long userId) {
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new BusinessException(ExceptionType.SEASON_NOT_FOUND));

        if (season.getStatus() == SeasonStatus.ACTIVE) {
            throw new BusinessException(ExceptionType.SEASON_NOT_ENDED);
        }

        List<DepartmentRankingTemp> aggregated = snapshotRepository
            .aggregateDepartmentRankingBySeasonId(seasonId);

        List<DepartmentRankingTemp> finalRankings = mergeAndRankDepartments(aggregated);

        return buildSeasonDepartmentRankingResponse(season, finalRankings, userId);
    }


    private List<PersonalRankingTemp> convertSnapshotsToRankings(List<SeasonRankingSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return Collections.emptyList();
        }

        // User ID 리스트 추출
        List<Long> userIds = snapshots.stream()
            .map(SeasonRankingSnapshot::getUserId)
            .collect(Collectors.toList());

        // User 정보 일괄 조회
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        return snapshots.stream()
            .map(snapshot -> {
                User user = userMap.get(snapshot.getUserId());
                if (user == null) {
                    return null;
                }
                return new PersonalRankingTemp(
                    user.getId(),
                    user.getNickname(),
                    user.getPicture(),
                    user.getDepartment() != null ? user.getDepartment().name() : null,
                    snapshot.getFinalTotalMillis(),
                    (long) snapshot.getFinalRank()
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    private List<DepartmentRankingTemp> mergeAndRankDepartments(List<DepartmentRankingTemp> allData) {
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> mergedMap = new HashMap<>();
        for (DepartmentRankingTemp data : allData) {
            mergedMap.merge(data.getDepartment(), data.getTotalMillis(), Long::sum);
        }

        List<Map.Entry<String, Long>> sorted = mergedMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList();

        List<DepartmentRankingTemp> result = new ArrayList<>();
        long currentRank = 1;
        Long previousMillis = null;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);

            if (previousMillis == null || !previousMillis.equals(entry.getValue())) {
                currentRank = i + 1;
            }

            result.add(new DepartmentRankingTemp(
                entry.getKey(),
                entry.getValue(),
                currentRank
            ));

            previousMillis = entry.getValue();
        }

        return result;
    }


    private SeasonDepartmentRankingResponse buildSeasonDepartmentRankingResponse(
            Season season, List<DepartmentRankingTemp> rankings, Long userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        DepartmentRankingEntryResponse myRanking = null;
        List<DepartmentRankingEntryResponse> topRanks = new ArrayList<>();

        for (DepartmentRankingTemp temp : rankings) {
            DepartmentRankingEntryResponse entry = DepartmentRankingEntryResponse.of(temp);

            if (temp.getTotalMillis() != null && temp.getTotalMillis() > 0) {
                topRanks.add(entry);
            }

            if (user.getDepartment() != null && user.getDepartment().getKoreanName().equals(temp.getDepartmentName())) {
                myRanking = entry;
            }
        }

        if (myRanking == null && user.getDepartment() != null) {
            myRanking = new DepartmentRankingEntryResponse(
                user.getDepartment().getKoreanName(),
                0L,
                (long) topRanks.size() + 1
            );
        }

        return SeasonDepartmentRankingResponse.of(season, topRanks, myRanking);
    }


    private List<PersonalRankingTemp> mergeAndRank(List<PersonalRankingTemp> allData) {
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        // 1단계: userId별로 totalMillis 합산
        Map<Long, PersonalRankingTemp> mergedMap = new HashMap<>();
        for (PersonalRankingTemp data : allData) {
            mergedMap.merge(
                data.getUserId(),
                data,
                (existing, newData) -> new PersonalRankingTemp(
                    existing.getUserId(),
                    existing.getNickname(),
                    existing.getImageUrl(),
                    existing.getDepartment(),
                    existing.getTotalMillis() + newData.getTotalMillis(),
                    0L
                )
            );
        }

        // 2단계: totalMillis 내림차순 정렬
        List<PersonalRankingTemp> sorted = mergedMap.values().stream()
            .sorted(Comparator.comparing(PersonalRankingTemp::getTotalMillis).reversed())
            .toList();

        // 3단계: 동점자 처리하며 순위 부여 (MySQL RANK() 함수와 동일)
        List<PersonalRankingTemp> result = new ArrayList<>();
        long currentRank = 1;
        Long previousMillis = null;

        for (int i = 0; i < sorted.size(); i++) {
            PersonalRankingTemp temp = sorted.get(i);

            // 동점자가 아니면 실제 순위(i+1)를 부여
            if (previousMillis == null || !previousMillis.equals(temp.getTotalMillis())) {
                currentRank = i + 1;
            }
            // 동점자면 이전 순위 유지

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
