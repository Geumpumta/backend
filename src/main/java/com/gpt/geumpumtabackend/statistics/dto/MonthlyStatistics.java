package com.gpt.geumpumtabackend.statistics.dto;

public interface MonthlyStatistics {
    Long getTotalMonthMillis();        // 총 공부시간(ms)
    Integer getAverageDailyMillis();      // 월 일수로 나눈 일일 평균(ms)
    Integer getMaxConsecutiveStudyDays();  // 해당 월 내 최장 연속 공부 일수
    Integer getStudiedDays();              // 이번 달 공부 일수(>0ms인 날의 수)
}
