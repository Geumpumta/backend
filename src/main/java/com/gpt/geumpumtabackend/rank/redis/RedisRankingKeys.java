package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.user.domain.Department;

/**
 * Redis 실시간 랭킹 키 생성 클래스.
 *
 * 키 모델:
 * - done ZSET: 종료된 공부 시간만 저장한다. score = 누적 완료 공부 시간(ms).
 * - active ZSET: 현재 공부 중인 사용자를 저장한다.
 *   score = 활성 세션 시작 전 누적 공부 시간(ms) - 공부 반영 시작 시각(epoch ms).
 * - active session HASH: 세션 종료 시 현재 종료되는 세션이 Redis의 활성 세션과 같은지 검증하기 위한 메타데이터.
 * - active users SET: 자정, 주간, 월간 경계 처리 대상인 활성 사용자 목록.
 *
 * 공부 중인 사용자를 done과 분리해서 저장하기 때문에 1초마다 Redis에 쓰지 않아도 된다.
 * 조회 시 active score에 현재 시각(epoch ms)을 더해서 실시간 점수로 변환한다.
 */
final class RedisRankingKeys {

    static final String PREFIX = "rank:v1";
    static final String ACTIVE_USERS = PREFIX + ":active:users";
    static final String ACTIVE_SESSION_PREFIX = PREFIX + ":active:session:";

    private RedisRankingKeys() {
    }

    static String userDone(RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:" + period.keyToken() + ":" + periodId + ":done";
    }

    /**
     * 기간별 개인 active 랭킹.
     * member = userId
     * score = 활성 세션 시작 전 완료 점수 - 공부 반영 시작 시각(epoch ms)
     */
    static String userActive(RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:" + period.keyToken() + ":" + periodId + ":active";
    }

    /**
     * 기간별 학과 done 랭킹.
     * 학과 랭킹은 학과별 상위 30명의 합으로 계산하므로, 학과 키 안에도 사용자별 점수를 저장한다.
     */
    static String departmentDone(Department department, RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:dept:" + department.name() + ":" + period.keyToken() + ":" + periodId + ":done";
    }

    /**
     * 기간별 학과 active 랭킹.
     * 개인 active 키와 같은 구조지만, 학과별 top30 합산을 빠르게 하기 위해 학과 단위로 나눈다.
     */
    static String departmentActive(Department department, RedisRankingPeriod period, String periodId) {
        return PREFIX + ":user:dept:" + department.name() + ":" + period.keyToken() + ":" + periodId + ":active";
    }

    /**
     * 활성 세션 메타데이터 HASH 키.
     * 필드: sessionId, startAtMs, department.
     */
    static String activeSession(long userId) {
        return ACTIVE_SESSION_PREFIX + userId;
    }
}
