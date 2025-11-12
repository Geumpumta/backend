package com.gpt.geumpumtabackend.statistics.dto.response;

import com.gpt.geumpumtabackend.statistics.dto.MonthlyStatistics;

public record MonthlyStatisticsResponse (
        MonthlyStatistics monthlyStatistics
){
    public static MonthlyStatisticsResponse from(MonthlyStatistics monthlyStatistics){
        return new MonthlyStatisticsResponse(monthlyStatistics);
    }
}
