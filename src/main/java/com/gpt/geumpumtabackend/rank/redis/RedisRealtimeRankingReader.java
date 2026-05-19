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
public class RedisRealtimeRankingReader {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int PERSONAL_TOP_K = 100;
    private static final int DEPARTMENT_TOP_USERS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    private final Cache<String, List<RankingScore>> personalTopCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(256)
            .build();

    private final Cache<String, List<DepartmentScore>> departmentTopCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(256)
            .build();

    public Optional<PersonalRankingResponse> personalDaily(Long userId) {
        return personal(RedisRankingPeriod.DAY, LocalDate.now(), userId);
    }

    public Optional<PersonalRankingResponse> personalWeekly(Long userId) {
        return personal(RedisRankingPeriod.WEEK, LocalDate.now(), userId);
    }

    public Optional<PersonalRankingResponse> personalMonthly(Long userId) {
        return personal(RedisRankingPeriod.MONTH, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentDaily(Long userId) {
        return department(RedisRankingPeriod.DAY, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentWeekly(Long userId) {
        return department(RedisRankingPeriod.WEEK, LocalDate.now(), userId);
    }

    public Optional<DepartmentRankingResponse> departmentMonthly(Long userId) {
        return department(RedisRankingPeriod.MONTH, LocalDate.now(), userId);
    }

    private Optional<PersonalRankingResponse> personal(RedisRankingPeriod period, LocalDate date, Long userId) {
        long nowMs = System.currentTimeMillis();
        String periodId = period.id(date);
        String doneKey = RedisRankingKeys.userDone(period, periodId);
        String activeKey = RedisRankingKeys.userActive(period, periodId);

        if (!hasRankingData(doneKey, activeKey)) {
            return Optional.empty();
        }

        String cacheKey = "personal:" + period.keyToken() + ":" + periodId + ":" + nowMs / 1000;
        List<RankingScore> topScores = personalTopCache.get(cacheKey,
                ignored -> topRankingScores(doneKey, activeKey, nowMs, PERSONAL_TOP_K));

        PersonalRankingEntryResponse myRanking = buildMyRanking(doneKey, activeKey, userId, nowMs, topScores.size());
        List<Long> userIds = new ArrayList<>(topScores.stream().map(RankingScore::userId).toList());
        if (userId != null && !userIds.contains(userId)) {
            userIds.add(userId);
        }
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

        if (!hasAnyDepartmentData(period, periodId)) {
            return Optional.empty();
        }

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
            scoreByUser.put(score.userId(), score.score());
        }
        for (RankingScore score : readActive(activeKey, nowMs, topK)) {
            scoreByUser.put(score.userId(), score.score());
        }

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
            long score = topRankingScores(doneKey, activeKey, nowMs, DEPARTMENT_TOP_USERS)
                    .stream()
                    .mapToLong(RankingScore::score)
                    .sum();
            scores.add(new DepartmentScore(department, score));
        }

        scores.sort(Comparator
                .comparingLong(DepartmentScore::score).reversed()
                .thenComparing(s -> s.department().name()));
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
        Double activeAdjusted = redisTemplate.opsForZSet().score(activeKey, member);
        long myScore;
        if (activeAdjusted != null) {
            myScore = Math.max(0L, Math.round(activeAdjusted + nowMs));
        } else {
            Double doneScore = redisTemplate.opsForZSet().score(doneKey, member);
            myScore = doneScore == null ? 0L : Math.max(0L, Math.round(doneScore));
        }

        long greaterDone = countGreater(doneKey, myScore);
        long greaterActive = countGreater(activeKey, myScore - nowMs);
        long rank = greaterDone + greaterActive + 1;
        if (myScore == 0L && greaterDone == 0L && greaterActive == 0L) {
            rank = currentTopSize + 1L;
        }

        return new PersonalRankingEntryResponse(userId, myScore, rank, null, null, null);
    }

    private List<RankingScore> readDone(String key, int limit) {
        return readZSet(key, limit, 0L);
    }

    private List<RankingScore> readActive(String key, long nowMs, int limit) {
        return readZSet(key, limit, nowMs);
    }

    private List<RankingScore> readZSet(String key, int limit, long scoreOffset) {
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
                rank = i + 2L;
            }
        }
        return rank;
    }

    private long countGreater(String key, double score) {
        Long count = redisTemplate.opsForZSet().count(key, Math.nextUp(score), Double.MAX_VALUE);
        return count == null ? 0L : count;
    }

    private boolean hasRankingData(String doneKey, String activeKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(doneKey))
                || Boolean.TRUE.equals(redisTemplate.hasKey(activeKey));
    }

    private boolean hasAnyDepartmentData(RedisRankingPeriod period, String periodId) {
        for (Department department : Department.values()) {
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
        return Comparator
                .comparingLong(RankingScore::score).reversed()
                .thenComparingLong(RankingScore::userId);
    }

    private Map<Long, User> resolveUsers(List<Long> userIds) {
        Map<Long, User> users = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return users;
        }
        for (User user : userRepository.findAllById(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private String departmentName(User user) {
        if (user == null || user.getDepartment() == null) {
            return null;
        }
        return user.getDepartment().getKoreanName();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record RankingScore(Long userId, Long score, Long rank) {
    }

    private record DepartmentScore(Department department, Long score) {
    }
}
