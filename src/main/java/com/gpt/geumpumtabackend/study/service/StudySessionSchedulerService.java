package com.gpt.geumpumtabackend.study.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        zombieSessions.forEach(studySession -> {
            try {
                studySession.endStudySession(now);
            } catch (Exception e) {
                throw new BusinessException(ExceptionType.ZOMBIE_SCHEDULER_ERROR);
            }
        });
        studySessionRepository.saveAll(zombieSessions);
    }
}
