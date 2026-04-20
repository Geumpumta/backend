package com.gpt.geumpumtabackend.rankbenchmark.redis;

import com.gpt.geumpumtabackend.user.domain.Department;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public final class RedisRankingKeys {

    public static final String USER_PREFIX = "rank:user";
    public static final String DEPT_PREFIX = "rank:dept";
    public static final String ACTIVE_SESSION_HASH = "active:session:";
    public static final String ACTIVE_USERS_SET = "active:users";
    public static final String ACTIVE_EXPIRY_ZSET = "active:expiry";

    private RedisRankingKeys() {
    }

    public static String userDay(LocalDate date) {
        return USER_PREFIX + ":day:" + date;
    }

    public static String userWeek(LocalDate weekAnchor) {
        return USER_PREFIX + ":week:" + isoWeek(weekAnchor);
    }

    public static String userMonth(LocalDate date) {
        return USER_PREFIX + ":month:" + String.format(Locale.ROOT, "%04d-%02d",
                date.getYear(), date.getMonthValue());
    }

    public static String userDeptDay(Department department, LocalDate date) {
        return USER_PREFIX + ":dept:" + department.name() + ":day:" + date;
    }

    public static String userDeptWeek(Department department, LocalDate weekAnchor) {
        return USER_PREFIX + ":dept:" + department.name() + ":week:" + isoWeek(weekAnchor);
    }

    public static String userDeptMonth(Department department, LocalDate date) {
        return USER_PREFIX + ":dept:" + department.name() + ":month:"
                + String.format(Locale.ROOT, "%04d-%02d", date.getYear(), date.getMonthValue());
    }

    public static String activeSessionHash(long userId) {
        return ACTIVE_SESSION_HASH + userId;
    }

    private static String isoWeek(LocalDate date) {
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        int week = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = monday.get(WeekFields.ISO.weekBasedYear());
        return String.format(Locale.ROOT, "%04d-W%02d", year, week);
    }
}
