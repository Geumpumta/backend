package com.gpt.geumpumtabackend.study.scheduler;

import com.gpt.geumpumtabackend.study.config.StudyProperties;
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
    private final StudyProperties studyProperties;

    @Scheduled(fixedRate = 1000)
    public void checkAndFinishMaxFocusSessions() {
        studySessionService.endExpiredMaxFocusSessions();
    }
}
