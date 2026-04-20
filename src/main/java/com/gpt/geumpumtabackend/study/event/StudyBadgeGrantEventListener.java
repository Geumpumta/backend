package com.gpt.geumpumtabackend.study.event;

import com.gpt.geumpumtabackend.badge.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudyBadgeGrantEventListener {

    private final BadgeService badgeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudySessionEnded(StudySessionEndedEvent event) {
        try {
            badgeService.grantStudyAchievementBadges(event.userId());
        } catch (Exception e) {
            log.warn("배지 지급 실패 - userId={}", event.userId(), e);
        }
    }
}
