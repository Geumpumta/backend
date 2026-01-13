package com.gpt.geumpumtabackend.statistics.dto.response;

import com.gpt.geumpumtabackend.statistics.dto.DayMaxFocusAndFullTimeStatistics;
import com.gpt.geumpumtabackend.statistics.dto.TwoHourSlotStatistics;

import java.util.List;

public record DailyStatisticsResponse(
        List<TwoHourSlotStatistics> statisticsList,
        DayMaxFocusAndFullTimeStatistics dayMaxFocusAndFullTimeStatistics
) {
    public static DailyStatisticsResponse from(List<TwoHourSlotStatistics> statisticsList, DayMaxFocusAndFullTimeStatistics dayMaxFocusAndFullTimeStatistics){
        return new DailyStatisticsResponse(statisticsList, dayMaxFocusAndFullTimeStatistics);
    }
}
