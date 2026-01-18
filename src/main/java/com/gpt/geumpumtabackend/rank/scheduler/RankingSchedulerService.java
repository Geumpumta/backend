package com.gpt.geumpumtabackend.rank.scheduler;

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
import com.gpt.geumpumtabackend.user.domain.Department;
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
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class RankingSchedulerService {

    private final StudySessionRepository studySessionRepository;
    private final UserRankingRepository userRankingRepository;
    private final UserRepository userRepository;
    private final DepartmentRankingRepository departmentRankingRepository;


    @Scheduled(cron = "5 0 0 * * *")
    public void dailyRankingScheduler() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime dayStart = yesterday.atStartOfDay();
        LocalDateTime dayEnd = yesterday.atTime(23, 59, 59);
        calculateAndSavePersonalRanking(dayStart, dayEnd, RankingType.DAILY);
        calculateAndSaveDepartmentRanking(dayStart, dayEnd, RankingType.DAILY);
    }


    @Scheduled(cron = "0 1 0 ? * MON")
    public void weeklyRankingScheduler() {
        LocalDate today = LocalDate.now();
        LocalDate lastWeekStartDay = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekEndDay = today.minusWeeks(1).with(DayOfWeek.SUNDAY);

        LocalDateTime weekStartTime = lastWeekStartDay.atStartOfDay();
        LocalDateTime weekEndTime = lastWeekEndDay.atTime(23, 59, 59);
        calculateAndSavePersonalRanking(weekStartTime, weekEndTime, RankingType.WEEKLY);
        calculateAndSaveDepartmentRanking(weekStartTime, weekEndTime, RankingType.WEEKLY);
    }


    @Scheduled(cron = "0 2 0 1 * ?")
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


        List<Long> userIds = userRankingTemps.stream()
                .map(PersonalRankingTemp::getUserId)
                .toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<UserRanking> userRankings = userRankingTemps.stream().map(
                dto -> {
                    User user = userMap.get(dto.getUserId());
                    if (user == null) {
                        throw new BusinessException(ExceptionType.USER_NOT_FOUND);
                    }
                    return UserRanking.builder()
                            .user(user)  // 영속성 컨텍스트에서 관리되는 실제 User 엔티티
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
                            .department(Department.valueOf(dto.getDepartment()))  // String → Department 변환
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
