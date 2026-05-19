package com.gpt.geumpumtabackend.rank.redis;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Redis 실시간 랭킹에서 지원하는 기간 단위.
 *
 * writer, reader, rollover, rebuild 로직이 같은 기준으로 기간을 계산해야 하므로
 * Redis 키에 들어갈 기간 id와 기간 경계를 이 enum에서 함께 관리한다.
 */
enum RedisRankingPeriod {
    DAY {
        @Override
        String id(LocalDate date) {
            return date.toString();
        }

        @Override
        LocalDateTime start(LocalDate date) {
            return date.atStartOfDay();
        }

        @Override
        LocalDateTime nextStart(LocalDate date) {
            return date.plusDays(1).atStartOfDay();
        }
    },
    WEEK {
        @Override
        String id(LocalDate date) {
            LocalDate monday = date.with(DayOfWeek.MONDAY);
            int week = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
            int year = monday.get(WeekFields.ISO.weekBasedYear());
            return String.format(Locale.ROOT, "%04d-W%02d", year, week);
        }

        @Override
        LocalDateTime start(LocalDate date) {
            return date.with(DayOfWeek.MONDAY).atStartOfDay();
        }

        @Override
        LocalDateTime nextStart(LocalDate date) {
            return date.with(DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay();
        }
    },
    MONTH {
        @Override
        String id(LocalDate date) {
            return String.format(Locale.ROOT, "%04d-%02d", date.getYear(), date.getMonthValue());
        }

        @Override
        LocalDateTime start(LocalDate date) {
            return date.withDayOfMonth(1).atStartOfDay();
        }

        @Override
        LocalDateTime nextStart(LocalDate date) {
            return date.withDayOfMonth(1).plusMonths(1).atStartOfDay();
        }
    };

    /**
     * Redis 키에 들어가는 기간 id.
     * DAY   -> 2026-05-19
     * WEEK  -> 2026-W21
     * MONTH -> 2026-05
     */
    abstract String id(LocalDate date);

    /**
     * 서버 로컬 시간 기준 기간 시작 시각.
     */
    abstract LocalDateTime start(LocalDate date);

    /**
     * 다음 기간의 시작 시각.
     * 공부 세션이 일/주/月 경계를 넘을 때 세그먼트를 나누는 기준으로 사용한다.
     */
    abstract LocalDateTime nextStart(LocalDate date);

    /**
     * Redis 키에 들어가는 소문자 기간 토큰.
     */
    String keyToken() {
        return name().toLowerCase(Locale.ROOT);
    }
}
