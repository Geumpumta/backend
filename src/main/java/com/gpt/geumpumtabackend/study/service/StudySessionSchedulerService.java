package com.gpt.geumpumtabackend.study.service;

import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionSchedulerService {

    private final StudySessionRepository studySessionRepository;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void cleanZombieStudySession() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(90);
        List<StudySession> zombieSessions = studySessionRepository.findAllZombieSession(threshold);

        List<StudySession> zombieSession = new ArrayList<>();
        zombieSessions.forEach(studySession -> {
            try {
                studySession.endStudySession(now);
                zombieSession.add(studySession);
            } catch (Exception e) {
               log.error("Failed to end zombie session: {}", studySession.getId(), e);
            }
        });
        if (!zombieSession.isEmpty()) {
            studySessionRepository.saveAll(zombieSession);
        }
    }
}
