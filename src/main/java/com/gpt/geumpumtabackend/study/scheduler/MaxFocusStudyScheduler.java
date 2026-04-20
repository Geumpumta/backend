package com.gpt.geumpumtabackend.study.scheduler;

import com.gpt.geumpumtabackend.study.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaxFocusStudyScheduler {

    private final StudySessionService studySessionService;

    @Scheduled(fixedRate = 1000)
    public void checkAndFinishMaxFocusSessions() {
        try {
            studySessionService.endExpiredMaxFocusSessions();
        } catch (Exception e) {
            log.error("[MAX_FOCUS_SCHEDULER] Failed to check max focus sessions", e);
        }
    }
}
