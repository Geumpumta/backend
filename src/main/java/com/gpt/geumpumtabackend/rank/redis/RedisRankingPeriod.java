package com.gpt.geumpumtabackend.rank.redis;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

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

    abstract String id(LocalDate date);

    abstract LocalDateTime start(LocalDate date);

    abstract LocalDateTime nextStart(LocalDate date);

    String keyToken() {
        return name().toLowerCase(Locale.ROOT);
    }
}
