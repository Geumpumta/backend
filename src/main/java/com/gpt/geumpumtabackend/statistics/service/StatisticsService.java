package com.gpt.geumpumtabackend.statistics.service;

import com.gpt.geumpumtabackend.statistics.dto.DayMaxFocusAndFullTimeStatistics;
import com.gpt.geumpumtabackend.statistics.dto.MonthlyStatistics;
import com.gpt.geumpumtabackend.statistics.dto.TwoHourSlotStatistics;
import com.gpt.geumpumtabackend.statistics.dto.WeeklyStatistics;
import com.gpt.geumpumtabackend.statistics.dto.response.DailyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.GrassStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.MonthlyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.WeeklyStatisticsResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
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
    private final ZoneId zone = ZoneId.of("Asia/Seoul");

    /**
     * 일간 통계 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public DailyStatisticsResponse getDailyStatistics(
            LocalDate date,
            Long userId
    ){
        LocalDateTime dayStart = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime dayEnd   = dayStart.plusDays(1);
        List<TwoHourSlotStatistics> stats = getTwoHourSlots(dayStart, dayEnd, userId);
        DayMaxFocusAndFullTimeStatistics dayMaxFocusAndFullTime = getDayMaxFocusStatistics(dayStart, dayEnd, userId);
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
            Long userId
    ){
        LocalDateTime weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toLocalDateTime();
        return WeeklyStatisticsResponse.from(getWeeklyStatistics(weekStart, userId));
    }

    /**
     * 월간 통계 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public MonthlyStatisticsResponse getMonthlyStatistics(
            LocalDate date,
            Long userId
    ){
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay(zone).toLocalDateTime();
        return MonthlyStatisticsResponse.from(getMonthlyStatistics(monthStart, userId));
    }

    /**
     * 잔디 얻어오는 메서드
     * @param date
     * @param userId
     * @return
     */
    public GrassStatisticsResponse getGrassStatistics(
            LocalDate date,
            Long userId
    ){
        LocalDate firstDayOfMonth = date.minusMonths(3).withDayOfMonth(1);
        LocalDate endOfMonth = date.plusMonths(1).withDayOfMonth(1);
        return GrassStatisticsResponse.from(studySessionRepository.getGrassStatistics(firstDayOfMonth, endOfMonth, userId));
    }

    public List<TwoHourSlotStatistics> getTwoHourSlots(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            Long userId
    ){
        return studySessionRepository.getTwoHourSlotStats(dayStart, dayEnd, userId);
    }

    public DayMaxFocusAndFullTimeStatistics getDayMaxFocusStatistics(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            Long userId
    ){
        return studySessionRepository.getDayMaxFocusAndFullTime(dayStart, dayEnd, userId);
    }

    public WeeklyStatistics getWeeklyStatistics(
            LocalDateTime weekStart,
            Long userId
    ){
        return studySessionRepository.getWeeklyStatistics(weekStart, userId);
    }


    public MonthlyStatistics getMonthlyStatistics(
            LocalDateTime monthStart,
            Long userId
    ){
        return studySessionRepository.getMonthlyStatistics(monthStart, userId);
    }

}
