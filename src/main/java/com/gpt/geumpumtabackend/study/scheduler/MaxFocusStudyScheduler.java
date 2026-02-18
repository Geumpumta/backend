package com.gpt.geumpumtabackend.study.scheduler;

import com.gpt.geumpumtabackend.fcm.service.FcmService;
import com.gpt.geumpumtabackend.study.config.StudyProperties;
import com.gpt.geumpumtabackend.study.service.StudySessionService;
import com.gpt.geumpumtabackend.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaxFocusStudyScheduler {

    private final StudySessionService studySessionService;
    private final FcmService fcmService;
    private final StudyProperties studyProperties;

    @Scheduled(fixedRate = 1000)
    public void checkAndFinishMaxFocusSessions() {
        try {
            List<User> usersToNotify = studySessionService.endExpiredMaxFocusSessions();

            int maxFocusHours = studyProperties.getMaxFocusHours();
            for (User user : usersToNotify) {
                try {
                    fcmService.sendMaxFocusNotification(user, maxFocusHours);
                } catch (Exception e) {
                    log.error("Failed to send FCM max focus notification for user {}", user.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("[MAX_FOCUS_SCHEDULER] Failed to check max focus sessions", e);
        }
    }
}
