package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.study.event.StudySessionChanged;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("rank-read-redis")
public class RedisDailyRanking {
    record Candidate(long userId, long micros) {}

    private static final String SOURCE = """
        SELECT u.id, COUNT(CASE WHEN s.id IS NOT NULL AND s.end_time IS NULL THEN 1 END) AS active,
               COALESCE(SUM(CASE WHEN s.end_time IS NULL
                   THEN -TIMESTAMPDIFF(MICROSECOND, :start, GREATEST(s.start_time, :start))
                   ELSE TIMESTAMPDIFF(MICROSECOND, GREATEST(s.start_time, :start), LEAST(s.end_time, :end))
               END), 0) AS score
        FROM user u LEFT JOIN study_session s ON s.user_id = u.id AND s.start_time <= :end
             AND (s.end_time >= :start OR s.end_time IS NULL)
        WHERE u.role = 'USER'
        """;

    private static final DefaultRedisScript<Long> MOVE = new DefaultRedisScript<>("""
        redis.call('ZREM', KEYS[1], ARGV[1])
        redis.call('ZADD', KEYS[2], ARGV[2], ARGV[1])
        redis.call('EXPIRE', KEYS[2], 172800)
        return 1
        """, Long.class);

    private static final DefaultRedisScript<List> TOP = new DefaultRedisScript<>("""
        local candidates = redis.call('ZREVRANGE', KEYS[1], 0, 99, 'WITHSCORES')
        local active = redis.call('ZREVRANGE', KEYS[2], 0, 99, 'WITHSCORES')
        for i = 1, #active, 2 do
            table.insert(candidates, active[i])
            table.insert(candidates, tostring(tonumber(active[i+1]) + tonumber(ARGV[1])))
        end
        return candidates
        """, List.class);

    private final NamedParameterJdbcTemplate db;
    private final StringRedisTemplate redis;
    private final UserRepository users;
    private final Set<Long> changed = ConcurrentHashMap.newKeySet();
    private final String namespace = "rank:experiment:" + UUID.randomUUID();
    private volatile LocalDate initialized;

    public RedisDailyRanking(NamedParameterJdbcTemplate db, StringRedisTemplate redis, UserRepository users) {
        this.db = db;
        this.redis = redis;
        this.users = users;
    }

    @TransactionalEventListener
    public void afterCommit(StudySessionChanged event) {
        changed.add(event.userId());
    }

    @Scheduled(fixedDelay = 100)
    public synchronized void refresh() {
        LocalDate day = initialized;
        if (day == null || changed.isEmpty()) return;
        for (long id : List.copyOf(changed)) {
            changed.remove(id);
            try {
                load(day, id);
            } catch (RuntimeException failure) {
                changed.add(id);
                throw failure;
            }
        }
    }

    private List<String> keys(LocalDate day) {
        String prefix = namespace + ":{" + day + "}:";
        return List.of(prefix + "idle", prefix + "active");
    }

    private synchronized void initialize(LocalDate day) {
        if (day.equals(initialized)) return;
        redis.delete(keys(day));
        load(day, null);
        initialized = day;
    }

    private void load(LocalDate day, Long id) {
        var params = new MapSqlParameterSource("start", day.atStartOfDay())
                .addValue("end", day.atTime(23, 59, 59)).addValue("id", id);
        var keys = keys(day);
        String sql = SOURCE + (id == null ? "" : " AND u.id = :id") + " GROUP BY u.id";
        db.query(sql, params, rs -> {
            int active = rs.getInt("active");
            if (active > 1) throw new IllegalStateException("Multiple active sessions for " + rs.getLong("id"));
            String user = rs.getString("id");
            String score = rs.getString("score");
            if (id == null) {
                redis.opsForZSet().add(keys.get(active), user, Double.parseDouble(score));
            } else {
                redis.execute(MOVE, List.of(keys.get(1 - active), keys.get(active)), user, score);
            }
        });
        if (id == null) keys.forEach(key -> redis.expire(key, Duration.ofDays(2)));
    }

    public List<PersonalRankingTemp> top(LocalDateTime now) {
        LocalDate day = now.toLocalDate();
        if (!day.equals(initialized)) initialize(day);
        long elapsed = Math.min(now.toLocalTime().toNanoOfDay() / 1000, 86_399_000_000L);
        List<?> raw = Objects.requireNonNull(redis.execute(TOP, keys(day), Long.toString(elapsed)));
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < raw.size(); i += 2) {
            candidates.add(new Candidate(Long.parseLong(raw.get(i).toString()), Long.parseLong(raw.get(i+1).toString())));
        }
        var winners = candidates.stream().sorted(Comparator.comparingLong(Candidate::micros).reversed()
                .thenComparingLong(Candidate::userId)).limit(100).toList();
        Map<Long, User> profiles = new HashMap<>();
        users.findAllById(winners.stream().map(Candidate::userId).toList()).forEach(u -> profiles.put(u.getId(), u));
        List<PersonalRankingTemp> result = new ArrayList<>();
        long previous = Long.MIN_VALUE;
        long rank = 0;
        for (Candidate winner : winners) {
            if (previous != winner.micros()) rank = result.size() + 1L;
            previous = winner.micros();
            var user = profiles.get(winner.userId());
            result.add(new PersonalRankingTemp(user.getId(), user.getNickname(), user.getPicture(),
                    user.getDepartment(), Math.round(previous / 1000.0), rank));
        }
        return result;
    }
}
