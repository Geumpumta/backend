package com.gpt.geumpumtabackend.statistics.dto.response;

import com.gpt.geumpumtabackend.statistics.dto.GrassStatistics;

import java.util.List;

public record GrassStatisticsResponse(
        List<GrassStatistics> grassStatistics
) {
    public static GrassStatisticsResponse from(List<GrassStatistics> grassStatistics) {
        return new GrassStatisticsResponse(grassStatistics);
    }
}
