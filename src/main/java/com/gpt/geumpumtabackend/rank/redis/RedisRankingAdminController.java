package com.gpt.geumpumtabackend.rank.redis;

import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/internal/rank/redis")
@Profile({"local", "dev", "rank-test"})
@RequiredArgsConstructor
public class RedisRankingAdminController {

    private final RedisRealtimeRankingRebuildService rebuildService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 현재 일간/주간/월간 Redis 랭킹 조회 모델을 MySQL 기준으로 다시 만든다.
     *
     * 운영 트래픽용 API가 아니라 Redis 랭킹 검증/복구용 내부 API다.
     * prod 프로필에서는 컨트롤러 bean 자체가 뜨지 않는다.
     */
    @PostMapping("/rebuild")
    public ResponseEntity<ResponseBody<RedisRankingRebuildResponse>> rebuildCurrentPeriods() {
        // MySQL study_session 원본을 기준으로 Redis done/active ZSET과 active metadata를 재생성한다.
        rebuildService.rebuildCurrentPeriods();

        // rebuild 직후 바로 key 크기를 응답해서 redis-cli 없이도 적재 여부를 확인할 수 있게 한다.
        RedisRankingStatsResponse stats = currentStats();
        RedisRankingRebuildResponse response = new RedisRankingRebuildResponse(
                "DONE",
                "Redis realtime ranking read model was rebuilt from MySQL.",
                Instant.now(),
                stats
        );
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    /**
     * 현재 날짜 기준 Redis 랭킹 key 크기를 확인한다.
     *
     * k6 테스트 전 이 API에서 dailyDone 또는 dailyActive가 0보다 큰지 확인하면
     * Redis 조회 경로를 탈 준비가 되었는지 빠르게 판단할 수 있다.
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseBody<RedisRankingStatsResponse>> getCurrentStats() {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(currentStats()));
    }

    private RedisRankingStatsResponse currentStats() {
        LocalDate today = LocalDate.now();
        PeriodStats daily = periodStats(RedisRankingPeriod.DAY, today);
        PeriodStats weekly = periodStats(RedisRankingPeriod.WEEK, today);
        PeriodStats monthly = periodStats(RedisRankingPeriod.MONTH, today);
        long activeUsers = setSize(RedisRankingKeys.ACTIVE_USERS);

        return new RedisRankingStatsResponse(today.toString(), daily, weekly, monthly, activeUsers);
    }

    private PeriodStats periodStats(RedisRankingPeriod period, LocalDate date) {
        String periodId = period.id(date);
        String doneKey = RedisRankingKeys.userDone(period, periodId);
        String activeKey = RedisRankingKeys.userActive(period, periodId);

        return new PeriodStats(
                period.keyToken(),
                periodId,
                doneKey,
                activeKey,
                zsetSize(doneKey),
                zsetSize(activeKey),
                departmentStats(period, periodId)
        );
    }

    private List<DepartmentPeriodStats> departmentStats(RedisRankingPeriod period, String periodId) {
        return java.util.Arrays.stream(com.gpt.geumpumtabackend.user.domain.Department.values())
                .map(department -> {
                    String doneKey = RedisRankingKeys.departmentDone(department, period, periodId);
                    String activeKey = RedisRankingKeys.departmentActive(department, period, periodId);
                    return new DepartmentPeriodStats(
                            department.name(),
                            zsetSize(doneKey),
                            zsetSize(activeKey)
                    );
                })
                .toList();
    }

    private long zsetSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0L : size;
    }

    private long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    public record RedisRankingRebuildResponse(
            String status,
            String message,
            Instant rebuiltAt,
            RedisRankingStatsResponse stats
    ) {
    }

    public record RedisRankingStatsResponse(
            String date,
            PeriodStats daily,
            PeriodStats weekly,
            PeriodStats monthly,
            long activeUsers
    ) {
    }

    public record PeriodStats(
            String period,
            String periodId,
            String doneKey,
            String activeKey,
            long doneSize,
            long activeSize,
            List<DepartmentPeriodStats> departments
    ) {
    }

    public record DepartmentPeriodStats(
            String department,
            long doneSize,
            long activeSize
    ) {
    }
}
