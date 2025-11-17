package com.gpt.geumpumtabackend.statistics.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.statistics.dto.DayMaxFocusAndFullTimeStatistics;
import com.gpt.geumpumtabackend.statistics.dto.MonthlyStatistics;
import com.gpt.geumpumtabackend.statistics.dto.TwoHourSlotStatistics;
import com.gpt.geumpumtabackend.statistics.dto.WeeklyStatistics;
import com.gpt.geumpumtabackend.statistics.dto.response.DailyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.GrassStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.MonthlyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.WeeklyStatisticsResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final ZoneId zone = ZoneId.of("Asia/Seoul");

    /**
     * 일간 통계 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public DailyStatisticsResponse getDailyStatistics(
            LocalDate date,
            Long targetUserId,
            Long userId
    ){
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        LocalDateTime dayStart = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime dayEnd   = dayStart.plusDays(1);
        List<TwoHourSlotStatistics> stats = getTwoHourSlots(dayStart, dayEnd, targetUserId);
        DayMaxFocusAndFullTimeStatistics dayMaxFocusAndFullTime = getDayMaxFocusStatistics(dayStart, dayEnd, targetUserId);
        return DailyStatisticsResponse.from(stats, dayMaxFocusAndFullTime);
    }

    /**
     * 주간 통계 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public WeeklyStatisticsResponse getWeeklyStatistics(
            LocalDate date,
            Long targetUserId,
            Long userId
    ){
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        LocalDateTime weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toLocalDateTime();
        return WeeklyStatisticsResponse.from(getWeeklyStatistics(weekStart, targetUserId));
    }

    /**
     * 월간 통계 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public MonthlyStatisticsResponse getMonthlyStatistics(
            LocalDate date,
            Long targetUserId,
            Long userId
    ){
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay(zone).toLocalDateTime();
        return MonthlyStatisticsResponse.from(getMonthlyStatistics(monthStart, targetUserId));
    }

    /**
     * 잔디 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public GrassStatisticsResponse getGrassStatistics(
            LocalDate date,
            Long targetUserId,
            Long userId
    ){
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        LocalDate firstDayOfMonth = date.minusMonths(3).withDayOfMonth(1);
        LocalDate endOfMonth = date.plusMonths(1).withDayOfMonth(1);
        return GrassStatisticsResponse.from(studySessionRepository.getGrassStatistics(firstDayOfMonth, endOfMonth, targetUserId));
    }

    public List<TwoHourSlotStatistics> getTwoHourSlots(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            Long targetUserId
    ){
        return studySessionRepository.getTwoHourSlotStats(dayStart, dayEnd, targetUserId);
    }

    public DayMaxFocusAndFullTimeStatistics getDayMaxFocusStatistics(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            Long targetUserId
    ){
        return studySessionRepository.getDayMaxFocusAndFullTime(dayStart, dayEnd, targetUserId);
    }

    public WeeklyStatistics getWeeklyStatistics(
            LocalDateTime weekStart,
            Long targetUserId
    ){
        return studySessionRepository.getWeeklyStatistics(weekStart, targetUserId);
    }


    public MonthlyStatistics getMonthlyStatistics(
            LocalDateTime monthStart,
            Long targetUserId
    ){
        return studySessionRepository.getMonthlyStatistics(monthStart, targetUserId);
    }

}
