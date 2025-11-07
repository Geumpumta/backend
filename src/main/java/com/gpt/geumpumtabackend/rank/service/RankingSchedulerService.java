package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.domain.UserRanking;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class RankingSchedulerService {

    private final StudySessionRepository studySessionRepository;
    private final UserRankingRepository userRankingRepository;
    private final UserRepository userRepository;
    private final DepartmentRankingRepository departmentRankingRepository;

    /*
    일간 랭킹 스케줄러
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyRankingScheduler() {
        // 해당 시간이 되면, StudySession에서 진행중인 세션을 종료하고, 모든 세션을 합하여 정렬한 뒤 랭킹에 넣어야함
        LocalDate yesterDay = LocalDate.now().minusDays(1);
        LocalDateTime dayStart = yesterDay.atStartOfDay();
        LocalDateTime dayEnd = yesterDay.atTime(23, 59, 59);
        calculateAndSavePersonalRanking(dayStart, dayEnd, RankingType.DAILY);
        calculateAndSaveDepartmentRanking(dayStart, dayEnd, RankingType.DAILY);

    }

    /*
    주간 랭킹 스케줄러
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void weeklyRankingScheduler() {
        LocalDate today = LocalDate.now();
        LocalDate lastWeekStartDay = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekEndDay = today.minusWeeks(1).with(DayOfWeek.SUNDAY);

        LocalDateTime weekStartTime = lastWeekStartDay.atStartOfDay();
        LocalDateTime weekEndTime = lastWeekEndDay.atTime(23, 59, 59);
        calculateAndSavePersonalRanking(weekStartTime, weekEndTime, RankingType.WEEKLY);
        calculateAndSaveDepartmentRanking(weekStartTime, weekEndTime, RankingType.WEEKLY);
    }

    /*
    월간 랭킹 스케줄러
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void monthlyRankingScheduler() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate monthStart = lastMonth.withDayOfMonth(1);
        LocalDate monthEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);
        calculateAndSavePersonalRanking(monthStartTime, monthEndTime, RankingType.MONTHLY);
        calculateAndSaveDepartmentRanking(monthStartTime, monthEndTime, RankingType.MONTHLY);
    }


    @Transactional
    public void calculateAndSavePersonalRanking(LocalDateTime periodStart, LocalDateTime periodEnd, RankingType rankingType) {
        List<PersonalRankingTemp> userRankingTemps = studySessionRepository.calculateFinalizedPeriodRanking(periodStart, periodEnd);

        List<UserRanking> userRankings = userRankingTemps.stream().map(
                dto -> {
                    User user = userRepository.findById(dto.getUserId())
                            .orElseThrow(()-> new BusinessException(ExceptionType.USER_NOT_FOUND));
                    return UserRanking.builder()
                            .user(user)
                            .totalMillis(dto.getTotalMillis())
                            .rank(dto.getRanking())
                            .rankingType(rankingType)
                            .calculatedAt(periodStart)
                            .build();
                })
                .collect(toList());
        userRankingRepository.saveAll(userRankings);
    }

    @Transactional
    public void calculateAndSaveDepartmentRanking(LocalDateTime periodStart, LocalDateTime periodEnd, RankingType rankingType) {
        List<DepartmentRankingTemp> departmentRankingTemps = studySessionRepository.calculateFinalizedDepartmentRanking(periodStart, periodEnd);

        List<DepartmentRanking> departmentRankings = departmentRankingTemps.stream().map(
                dto -> {
                    return DepartmentRanking.builder()
                            .department(dto.getDepartmentName())
                            .rank(dto.getRanking())
                            .totalMillis(dto.getTotalMillis())
                            .rankingType(rankingType)
                            .calculatedAt(periodStart)
                            .build();
                })
                .collect(toList());
        departmentRankingRepository.saveAll(departmentRankings);
    }
}
