package com.gpt.geumpumtabackend.rank.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Redis 실시간 랭킹 조회 모델을 조회하는 컴포넌트.
 *
 * 조회 정책:
 * - Redis 키가 있으면 Redis를 우선 사용한다.
 * - Redis 키가 없으면 Optional.empty()를 반환해서 서비스 계층이 기존 MySQL 쿼리로 대체 조회한다.
 * - active ZSET의 점수는 보정 점수이므로 조회 시 현재 시각(epoch ms)을 더해 실제 점수로 변환한다.
 *
 * Redis 부하 완화:
 * - 개인 top100과 학과 랭킹은 1초 Caffeine 캐시를 사용한다.
 * - 같은 초에 동시 요청이 몰려도 Redis topK 조회를 반복하지 않기 위한 목적이다.
 */
public class RedisRealtimeRankingReader {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int PERSONAL_TOP_K = 100;
    private static final int DEPARTMENT_TOP_USERS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    // 개인 랭킹 top100은 1초 동안만 캐싱한다. 실시간성 요구가 1초 단위이므로 더 길게 잡지 않는다.
    private final Cache<String, List<RankingScore>> personalTopCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(256)
            .build();

    // 학과 랭킹도 1초 캐싱한다. 내부적으로 학과별 top30을 합산하므로 개인 랭킹보다 Redis 조회 수가 많다.
    private final Cache<String, List<DepartmentScore>> departmentTopCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(256)
            .build();

    public Optional<PersonalRankingResponse> personalDaily(Long userId) {
        // 오늘 날짜 기준 일간 Redis key를 계산해서 개인 랭킹을 조회한다.
        return personal(RedisRankingPeriod.DAY, LocalDate.now(), userId);
    }

    public Optional<PersonalRankingResponse> personalWeekly(Long userId) {
        // 오늘이 속한 ISO 주차 기준 주간 Redis key를 계산해서 개인 랭킹을 조회한다.
        return personal(RedisRankingPeriod.WEEK, LocalDate.now(), userId);
    }

    public Optional<PersonalRankingResponse> personalMonthly(Long userId) {
        // 오늘이 속한 월 기준 월간 Redis key를 계산해서 개인 랭킹을 조회한다.
        return personal(RedisRankingPeriod.MONTH, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentDaily(Long userId) {
        // 오늘 날짜 기준 일간 Redis key를 계산해서 학과 랭킹을 조회한다.
        return department(RedisRankingPeriod.DAY, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentWeekly(Long userId) {
        // 오늘이 속한 ISO 주차 기준 주간 Redis key를 계산해서 학과 랭킹을 조회한다.
        return department(RedisRankingPeriod.WEEK, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentMonthly(Long userId) {
        // 오늘이 속한 월 기준 월간 Redis key를 계산해서 학과 랭킹을 조회한다.
        return department(RedisRankingPeriod.MONTH, LocalDate.now(), userId);
    }

    private Optional<PersonalRankingResponse> personal(RedisRankingPeriod period, LocalDate date, Long userId) {
        long nowMs = System.currentTimeMillis();
        String periodId = period.id(date);
        String doneKey = RedisRankingKeys.userDone(period, periodId);
        String activeKey = RedisRankingKeys.userActive(period, periodId);

        // Redis 조회 모델이 아직 없거나 유실된 경우 서비스 계층에서 MySQL 대체 조회를 사용한다.
        if (!hasRankingData(doneKey, activeKey)) {
            return Optional.empty();
        }

        // top100은 1초 단위로 캐싱해서 같은 초에 들어온 다수 요청의 Redis 부하를 줄인다.
        String cacheKey = "personal:" + period.keyToken() + ":" + periodId + ":" + nowMs / 1000;
        List<RankingScore> topScores = personalTopCache.get(cacheKey,
                ignored -> topRankingScores(doneKey, activeKey, nowMs, PERSONAL_TOP_K));

        // 내 랭킹은 top100 밖일 수 있으므로 top100 merge 결과와 별도로 score/rank를 계산한다.
        PersonalRankingEntryResponse myRanking = buildMyRanking(doneKey, activeKey, userId, nowMs, topScores.size());
        List<Long> userIds = new ArrayList<>(topScores.stream().map(RankingScore::userId).toList());
        if (userId != null && !userIds.contains(userId)) {
            // 내 랭킹이 top100 밖이어도 응답에는 내 프로필이 필요하므로 조회 대상에 추가한다.
            userIds.add(userId);
        }
        // Redis에는 userId와 score만 있으므로 사용자명, 이미지, 학과는 DB에서 한번에 조회한다.
        Map<Long, User> users = resolveUsers(userIds);

        List<PersonalRankingEntryResponse> topRanks = new ArrayList<>(topScores.size());
        for (RankingScore score : topScores) {
            User user = users.get(score.userId());
            topRanks.add(new PersonalRankingEntryResponse(
                    score.userId(),
                    score.score(),
                    score.rank(),
                    user != null ? user.getNickname() : null,
                    user != null ? user.getPicture() : null,
                    departmentName(user)
            ));
        }

        if (myRanking != null) {
            // buildMyRanking에서는 rank/score만 계산했으므로 프로필 정보를 채워 최종 응답으로 만든다.
            User user = users.get(myRanking.userId());
            myRanking = new PersonalRankingEntryResponse(
                    myRanking.userId(),
                    myRanking.totalMillis(),
                    myRanking.rank(),
                    user != null ? user.getNickname() : null,
                    user != null ? user.getPicture() : null,
                    departmentName(user)
            );
        }

        return Optional.of(new PersonalRankingResponse(topRanks, myRanking));
    }

    private Optional<DepartmentRankingResponse> department(RedisRankingPeriod period, LocalDate date, Long userId) {
        long nowMs = System.currentTimeMillis();
        String periodId = period.id(date);

        // 학과별 키가 하나도 없으면 Redis 조회 모델이 준비되지 않은 것으로 보고 MySQL로 대체 조회한다.
        if (!hasAnyDepartmentData(period, periodId)) {
            return Optional.empty();
        }

        // 학과 랭킹은 모든 학과의 top30 합산이 필요하므로 1초 캐시 효과가 크다.
        String cacheKey = "department:" + period.keyToken() + ":" + periodId + ":" + nowMs / 1000;
        List<DepartmentScore> scores = departmentTopCache.get(cacheKey,
                ignored -> departmentScores(period, periodId, nowMs));

        List<DepartmentRankingEntryResponse> topRanks = new ArrayList<>(scores.size());
        DepartmentRankingEntryResponse myRanking = null;
        Department myDepartment = userId == null
                ? null
                : userRepository.findById(userId).map(User::getDepartment).orElse(null);

        for (int i = 0; i < scores.size(); i++) {
            DepartmentScore score = scores.get(i);
            // 같은 점수는 같은 등수가 되도록 rankAt에서 앞선 학과 점수를 비교한다.
            DepartmentRankingEntryResponse entry = new DepartmentRankingEntryResponse(
                    score.department().getKoreanName(),
                    score.score(),
                    rankAt(scores, i)
            );
            topRanks.add(entry);
            if (myDepartment == score.department()) {
                myRanking = entry;
            }
        }

        if (myRanking == null && myDepartment != null) {
            // 내 학과가 점수 목록에 없으면 0점으로 마지막 다음 순위를 만들어 응답한다.
            myRanking = new DepartmentRankingEntryResponse(
                    myDepartment.getKoreanName(),
                    0L,
                    (long) scores.size() + 1
            );
        }

        return Optional.of(new DepartmentRankingResponse(topRanks, myRanking));
    }

    private List<RankingScore> topRankingScores(String doneKey, String activeKey, long nowMs, int topK) {
        Map<Long, Long> scoreByUser = new LinkedHashMap<>();
        for (RankingScore score : readDone(doneKey, topK)) {
            // 종료된 사용자의 점수는 이미 확정된 누적 ms이므로 그대로 사용한다.
            scoreByUser.put(score.userId(), score.score());
        }
        for (RankingScore score : readActive(activeKey, nowMs, topK)) {
            // 공부 중인 사용자의 점수는 readActive에서 현재 시각을 더해 실제 점수로 변환되어 들어온다.
            scoreByUser.put(score.userId(), score.score());
        }

        // done 상위 K명과 active 상위 K명을 합친 뒤 실제 점수 기준으로 다시 정렬한다.
        List<RankingScore> sorted = scoreByUser.entrySet().stream()
                .map(e -> new RankingScore(e.getKey(), e.getValue(), 0L))
                .sorted(scoreComparator())
                .limit(topK)
                .toList();
        return assignRanks(sorted);
    }

    private List<DepartmentScore> departmentScores(RedisRankingPeriod period, String periodId, long nowMs) {
        List<DepartmentScore> scores = new ArrayList<>(Department.values().length);
        for (Department department : Department.values()) {
            String doneKey = RedisRankingKeys.departmentDone(department, period, periodId);
            String activeKey = RedisRankingKeys.departmentActive(department, period, periodId);
            // 학과 점수는 학과 내 개인 상위 30명의 공부 시간 합이다.
            long score = topRankingScores(doneKey, activeKey, nowMs, DEPARTMENT_TOP_USERS)
                    .stream()
                    .mapToLong(RankingScore::score)
                    .sum();
            scores.add(new DepartmentScore(department, score));
        }

        scores.sort(Comparator
                .comparingLong(DepartmentScore::score).reversed()
                .thenComparing(s -> s.department().name()));
        // 모든 학과를 점수 내림차순으로 정렬해서 학과 랭킹 응답의 기본 순서를 만든다.
        return scores;
    }

    private PersonalRankingEntryResponse buildMyRanking(
            String doneKey,
            String activeKey,
            Long userId,
            long nowMs,
            int currentTopSize
    ) {
        if (userId == null) {
            return null;
        }

        String member = userId.toString();
        // 먼저 active ZSET을 확인한다. 공부 중이면 active 점수가 최신 점수 계산의 기준이다.
        Double activeAdjusted = redisTemplate.opsForZSet().score(activeKey, member);
        long myScore;
        if (activeAdjusted != null) {
            // 공부 중인 사용자의 실제 현재 점수 = Redis에 저장된 보정 점수 + 현재 시각.
            myScore = Math.max(0L, Math.round(activeAdjusted + nowMs));
        } else {
            Double doneScore = redisTemplate.opsForZSet().score(doneKey, member);
            myScore = doneScore == null ? 0L : Math.max(0L, Math.round(doneScore));
        }

        // 나보다 점수가 큰 done 사용자 수와 active 사용자 수를 더하면 내 등수를 계산할 수 있다.
        long greaterDone = countGreater(doneKey, myScore);
        // active ZSET은 보정 점수로 저장되어 있으므로 비교 기준도 내 점수 - 현재 시각으로 변환한다.
        long greaterActive = countGreater(activeKey, myScore - nowMs);
        long rank = greaterDone + greaterActive + 1;
        if (myScore == 0L && greaterDone == 0L && greaterActive == 0L) {
            // 0점 사용자가 아무 key에도 없으면 현재 top 목록 다음 순위로 응답한다.
            rank = currentTopSize + 1L;
        }

        return new PersonalRankingEntryResponse(userId, myScore, rank, null, null, null);
    }

    private List<RankingScore> readDone(String key, int limit) {
        // done ZSET은 저장 점수가 실제 점수이므로 offset 없이 읽는다.
        return readZSet(key, limit, 0L);
    }

    private List<RankingScore> readActive(String key, long nowMs, int limit) {
        // active ZSET은 보정 점수이므로 현재 시각을 offset으로 더해 실제 점수로 만든다.
        return readZSet(key, limit, nowMs);
    }

    private List<RankingScore> readZSet(String key, int limit, long scoreOffset) {
        // Redis ZSET을 점수 내림차순으로 limit개 읽는다.
        var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<RankingScore> scores = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            Long userId = parseLong(tuple.getValue());
            if (userId == null) {
                continue;
            }
            // done은 offset 0, active는 offset nowMs를 더해 실제 점수로 변환한다.
            long score = Math.max(0L, Math.round(tuple.getScore() + scoreOffset));
            scores.add(new RankingScore(userId, score, 0L));
        }
        return scores;
    }

    private List<RankingScore> assignRanks(List<RankingScore> sorted) {
        List<RankingScore> ranked = new ArrayList<>(sorted.size());
        long currentRank = 0;
        Long previousScore = null;
        for (int i = 0; i < sorted.size(); i++) {
            RankingScore score = sorted.get(i);
            if (previousScore == null || !previousScore.equals(score.score())) {
                // 이전 점수와 다를 때만 실제 순위를 현재 위치로 갱신한다. 같은 점수는 같은 등수다.
                currentRank = i + 1L;
                previousScore = score.score();
            }
            ranked.add(new RankingScore(score.userId(), score.score(), currentRank));
        }
        return ranked;
    }

    private long rankAt(List<DepartmentScore> scores, int index) {
        long rank = 1L;
        for (int i = 0; i < index; i++) {
            if (scores.get(i).score() > scores.get(index).score()) {
                // 앞에 있는 학과 중 현재 학과보다 점수가 큰 학과 수를 기준으로 순위를 결정한다.
                rank = i + 2L;
            }
        }
        return rank;
    }

    private long countGreater(String key, double score) {
        // Math.nextUp을 사용해 같은 점수는 제외하고, 오직 나보다 큰 점수만 센다.
        Long count = redisTemplate.opsForZSet().count(key, Math.nextUp(score), Double.MAX_VALUE);
        return count == null ? 0L : count;
    }

    private boolean hasRankingData(String doneKey, String activeKey) {
        // done 또는 active key 중 하나라도 있으면 Redis 조회 모델이 준비된 것으로 본다.
        return Boolean.TRUE.equals(redisTemplate.hasKey(doneKey))
                || Boolean.TRUE.equals(redisTemplate.hasKey(activeKey));
    }

    private boolean hasAnyDepartmentData(RedisRankingPeriod period, String periodId) {
        for (Department department : Department.values()) {
            // 학과별 done/active key를 순회하면서 하나라도 존재하는지 확인한다.
            if (hasRankingData(
                    RedisRankingKeys.departmentDone(department, period, periodId),
                    RedisRankingKeys.departmentActive(department, period, periodId)
            )) {
                return true;
            }
        }
        return false;
    }

    private Comparator<RankingScore> scoreComparator() {
        // 점수는 높은 순서, 같은 점수면 userId가 작은 순서로 고정해 응답 순서를 안정화한다.
        return Comparator
                .comparingLong(RankingScore::score).reversed()
                .thenComparingLong(RankingScore::userId);
    }

    private Map<Long, User> resolveUsers(List<Long> userIds) {
        Map<Long, User> users = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return users;
        }
        // Redis에는 사용자 프로필이 없으므로 필요한 userId만 모아 DB에서 배치 조회한다.
        for (User user : userRepository.findAllById(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private String departmentName(User user) {
        if (user == null || user.getDepartment() == null) {
            // 탈퇴, 누락, 학과 미등록 등으로 사용자 또는 학과가 없으면 null로 응답한다.
            return null;
        }
        return user.getDepartment().getKoreanName();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            // RedisTemplate 값 직렬화 결과는 문자열이므로 Long으로 변환한다.
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            // 잘못된 member 값은 랭킹 계산에서 제외한다.
            return null;
        }
    }

    private record RankingScore(Long userId, Long score, Long rank) {
    }

    private record DepartmentScore(Department department, Long score) {
    }
}
