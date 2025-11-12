package com.gpt.geumpumtabackend.statistics.dto;

public interface WeeklyStatistics {
    Long getTotalWeekSeconds();
    Integer getMaxConsecutiveStudyDays();
    Integer getAverageDailySeconds(); // 7일 평균(초), 소수점 버림
}
