package com.gpt.geumpumtabackend.rank.scheduler;

import com.gpt.geumpumtabackend.rank.domain.Season;
import com.gpt.geumpumtabackend.rank.service.SeasonService;
import com.gpt.geumpumtabackend.rank.service.SeasonSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeasonTransitionScheduler {

    private final SeasonService seasonService;
    private final SeasonSnapshotService snapshotService;
    private final CacheManager cacheManager;



    @Scheduled(cron = "0 5 0 * * *")
    public void processSeasonTransition() {
        LocalDate today = LocalDate.now();

        try {
            Season activeSeason = seasonService.getActiveSeasonNoCache();

            if (!today.equals(activeSeason.getEndDate().plusDays(1))) {
                return;
            }

            Long endedSeasonId = activeSeason.getId();


            // 캐시 먼저 클리어 (시즌 전환 전)
            if (cacheManager.getCache("activeSeason") != null) {
                cacheManager.getCache("activeSeason").clear();
            }

            // 시즌 전환
            seasonService.transitionToNextSeason(activeSeason);

            // 스냅샷 생성
            int snapshotCount = snapshotService.createSeasonSnapshot(endedSeasonId);
        } catch (Exception e) {
            log.error("[SEASON_TRANSITION_ERROR] Failed", e);
            // TODO: 슬랙/이메일 알림
        }
    }
}
