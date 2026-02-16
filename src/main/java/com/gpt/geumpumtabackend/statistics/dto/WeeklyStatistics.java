package com.gpt.geumpumtabackend.statistics.dto;

public interface WeeklyStatistics {
    Long getTotalWeekMillis();
    Integer getMaxConsecutiveStudyDays();
    Integer getAverageDailyMillis(); // 7일 평균(ms), 소수점 버림
}
