package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;

final class RedisRankingKeys {

    static final String PREFIX = "rank:v1";
    static final String ACTIVE_USERS = PREFIX + ":active:users";
    static final String ACTIVE_SESSION_PREFIX = PREFIX + ":active:session:";

    private RedisRankingKeys() {
    }

    static String userDone(RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:" + period.keyToken() + ":" + periodId + ":done";
    }

    static String userActive(RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:" + period.keyToken() + ":" + periodId + ":active";
    }

    static String departmentDone(Department department, RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:dept:" + department.name() + ":" + period.keyToken() + ":" + periodId + ":done";
    }

    static String departmentActive(Department department, RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:dept:" + department.name() + ":" + period.keyToken() + ":" + periodId + ":active";
    }

    static String activeSession(long userId) {
        return ACTIVE_SESSION_PREFIX + userId;
    }
}
