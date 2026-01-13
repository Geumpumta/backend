package com.gpt.geumpumtabackend.statistics.dto.response;

import com.gpt.geumpumtabackend.statistics.dto.WeeklyStatistics;

public record WeeklyStatisticsResponse(
        WeeklyStatistics weeklyStatistics
) {
    public static WeeklyStatisticsResponse from(WeeklyStatistics weeklyStatistics){
        return new WeeklyStatisticsResponse(weeklyStatistics);
    }
}
