package com.gpt.geumpumtabackend.statistics.dto;

public interface MonthlyStatistics {
    Long getTotalMonthSeconds();        // 총 공부시간(초)
    Integer getAverageDailySeconds();      // 월 일수로 나눈 일일 평균(초)
    Integer getMaxConsecutiveStudyDays();  // 해당 월 내 최장 연속 공부 일수
    Integer getStudiedDays();              // 이번 달 공부 일수(>0초인 날의 수)
}
