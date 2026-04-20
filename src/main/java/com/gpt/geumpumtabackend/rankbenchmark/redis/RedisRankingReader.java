package com.gpt.geumpumtabackend.rankbenchmark.redis;

import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Profile("local")
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class RedisRankingReader {

    private static final int PERSONAL_TOP_K = 100;
    private static final int DEPT_SIZE = Department.values().length;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    @SuppressWarnings("rawtypes")
    private DefaultRedisScript<List> personalScript;
    @SuppressWarnings("rawtypes")
    private DefaultRedisScript<List> departmentScript;

    @PostConstruct
    void init() {
        personalScript = new DefaultRedisScript<>();
        personalScript.setLocation(new ClassPathResource("scripts/rankbenchmark/personal_ranking.lua"));
        personalScript.setResultType(List.class);

        departmentScript = new DefaultRedisScript<>();
        departmentScript.setLocation(new ClassPathResource("scripts/rankbenchmark/department_ranking.lua"));
        departmentScript.setResultType(List.class);
    }

    public PersonalRankingResponse personalDaily(Long userId) {
        return runPersonal(RedisRankingKeys.userDay(LocalDate.now()), userId);
    }

    public PersonalRankingResponse personalWeekly(Long userId) {
        return runPersonal(RedisRankingKeys.userWeek(LocalDate.now()), userId);
    }

    public PersonalRankingResponse personalMonthly(Long userId) {
        return runPersonal(RedisRankingKeys.userMonth(LocalDate.now()), userId);
    }

    public DepartmentRankingResponse departmentDaily(Long userId) {
        return runDepartment(
                d -> RedisRankingKeys.userDeptDay(d, LocalDate.now()), userId);
    }

    public DepartmentRankingResponse departmentWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        return runDepartment(
                d -> RedisRankingKeys.userDeptWeek(d, today), userId);
    }

    public DepartmentRankingResponse departmentMonthly(Long userId) {
        LocalDate today = LocalDate.now();
        return runDepartment(
                d -> RedisRankingKeys.userDeptMonth(d, today), userId);
    }

    @SuppressWarnings("unchecked")
    private PersonalRankingResponse runPersonal(String periodKey, Long userId) {
        long nowMs = System.currentTimeMillis();
        List<String> keys = List.of(periodKey, RedisRankingKeys.ACTIVE_USERS_SET);
        List<Object> raw = redisTemplate.execute(
                personalScript,
                (List) keys,
                String.valueOf(nowMs),
                String.valueOf(PERSONAL_TOP_K),
                userId == null ? "" : userId.toString());

        if (raw == null || raw.isEmpty()) {
            return new PersonalRankingResponse(List.of(), fallbackMy(userId, 0));
        }

        int totalUsers = parseInt(raw.get(0));
        int topCount = parseInt(raw.get(1));

        List<Long> userIds = new ArrayList<>(topCount);
        List<Long> scores = new ArrayList<>(topCount);
        for (int i = 0; i < topCount; i++) {
            userIds.add(Long.parseLong(String.valueOf(raw.get(2 + i * 2))));
            scores.add((long) Double.parseDouble(String.valueOf(raw.get(3 + i * 2))));
        }
        long myRank = Long.parseLong(String.valueOf(raw.get(2 + topCount * 2)));
        long myScore = (long) Double.parseDouble(String.valueOf(raw.get(3 + topCount * 2)));

        Map<Long, User> byId = resolveUsers(userIds, userId);
        List<PersonalRankingEntryResponse> topRanks = new ArrayList<>(topCount);
        PersonalRankingEntryResponse my = null;

        for (int i = 0; i < topCount; i++) {
            Long uid = userIds.get(i);
            User u = byId.get(uid);
            String dep = (u != null && u.getDepartment() != null) ? u.getDepartment().getKoreanName() : null;
            PersonalRankingEntryResponse entry = new PersonalRankingEntryResponse(
                    uid,
                    scores.get(i),
                    (long) (i + 1),
                    u != null ? u.getNickname() : null,
                    u != null ? u.getPicture() : null,
                    dep);
            topRanks.add(entry);
            if (userId != null && userId.equals(uid)) {
                my = entry;
            }
        }

        if (my == null && userId != null) {
            User u = byId.get(userId);
            String dep = (u != null && u.getDepartment() != null) ? u.getDepartment().getKoreanName() : null;
            my = new PersonalRankingEntryResponse(
                    userId, myScore, myRank,
                    u != null ? u.getNickname() : null,
                    u != null ? u.getPicture() : null,
                    dep);
        }

        return new PersonalRankingResponse(topRanks, my);
    }

    @SuppressWarnings("unchecked")
    private DepartmentRankingResponse runDepartment(DeptKeyFn keyFn, Long userId) {
        long nowMs = System.currentTimeMillis();
        Department[] departments = Department.values();

        List<String> keys = new ArrayList<>(1 + departments.length);
        keys.add(RedisRankingKeys.ACTIVE_USERS_SET);
        for (Department d : departments) keys.add(keyFn.apply(d));

        Object[] args = new Object[2 + departments.length];
        args[0] = String.valueOf(nowMs);
        args[1] = String.valueOf(departments.length);
        for (int i = 0; i < departments.length; i++) {
            args[2 + i] = departments[i].name();
        }

        List<Object> raw = redisTemplate.execute(departmentScript, (List) keys, args);
        if (raw == null || raw.isEmpty()) {
            return new DepartmentRankingResponse(List.of(), null);
        }

        int count = parseInt(raw.get(0));
        List<DepartmentRankingEntryResponse> topRanks = new ArrayList<>();
        DepartmentRankingEntryResponse my = null;
        String myDeptEnum = null;
        if (userId != null) {
            myDeptEnum = userRepository.findById(userId)
                    .map(u -> u.getDepartment() != null ? u.getDepartment().name() : null)
                    .orElse(null);
        }

        for (int i = 0; i < count; i++) {
            String deptEnumName = String.valueOf(raw.get(1 + i * 2));
            long score = (long) Double.parseDouble(String.valueOf(raw.get(2 + i * 2)));
            Department dept;
            try {
                dept = Department.valueOf(deptEnumName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            DepartmentRankingEntryResponse entry = new DepartmentRankingEntryResponse(
                    dept.getKoreanName(), score, (long) (i + 1));
            if (score > 0) topRanks.add(entry);
            if (myDeptEnum != null && myDeptEnum.equals(deptEnumName)) my = entry;
        }

        return new DepartmentRankingResponse(topRanks, my);
    }

    private Map<Long, User> resolveUsers(List<Long> userIds, Long myUserId) {
        List<Long> toLoad = new ArrayList<>(userIds);
        if (myUserId != null && !toLoad.contains(myUserId)) toLoad.add(myUserId);
        Map<Long, User> map = new HashMap<>();
        if (toLoad.isEmpty()) return map;
        for (User u : userRepository.findAllById(toLoad)) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private PersonalRankingEntryResponse fallbackMy(Long userId, int currentSize) {
        if (userId == null) return null;
        User u = userRepository.findById(userId).orElse(null);
        String dep = (u != null && u.getDepartment() != null) ? u.getDepartment().getKoreanName() : null;
        return new PersonalRankingEntryResponse(
                userId, 0L, (long) currentSize + 1,
                u != null ? u.getNickname() : null,
                u != null ? u.getPicture() : null,
                dep);
    }

    private int parseInt(Object obj) {
        return Integer.parseInt(String.valueOf(obj));
    }

    @FunctionalInterface
    private interface DeptKeyFn {
        String apply(Department department);
    }
}
