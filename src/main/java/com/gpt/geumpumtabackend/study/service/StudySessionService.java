package com.gpt.geumpumtabackend.study.service;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.study.config.StudyProperties;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.domain.StudyStatus;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudySessionResponse;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.event.StudySessionEndedEvent;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final CampusWiFiValidationService wifiValidationService;
    private final StudyProperties studyProperties;
    private final ApplicationEventPublisher eventPublisher;

    /*
    메인 홈
     */
    public StudySessionResponse getTodayStudySession(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        StudySession activeSession = studySessionRepository.findByUser_IdAndStatus(userId, StudyStatus.STARTED)
                .orElse(null);
        boolean isStudying = activeSession != null;
        LocalDateTime startTime = isStudying ? activeSession.getStartTime() : null;
        Long totalStudySession = studySessionRepository.sumCompletedStudySessionByUserId(userId, startOfDay, now);
        return StudySessionResponse.of(totalStudySession, isStudying, startTime);
    }

    /*
    공부 시작
     */
    @Transactional
    public StudyStartResponse startStudySession(StudyStartRequest request, Long userId) {
        verifyCampusWifiConnection(request, userId);
        StudySession savedSession = makeStudySession(userId);
        return StudyStartResponse.fromEntity(savedSession);
    }

    /*
    공부 종료
     */
    @Transactional
    public void endStudySession(StudyEndRequest endRequest, Long userId) {
        StudySession studysession = studySessionRepository.findByIdAndUser_Id(endRequest.studySessionId(), userId)
                .orElseThrow(()->new BusinessException(ExceptionType.STUDY_SESSION_NOT_FOUND));
        LocalDateTime endTime = LocalDateTime.now();
        studysession.endStudySession(endTime);
        eventPublisher.publishEvent(new StudySessionEndedEvent(userId));
    }

    private BusinessException mapWiFiValidationException(WiFiValidationResult result) {
        return switch (result.getStatus()) {
            case INVALID -> new BusinessException(ExceptionType.WIFI_NOT_CAMPUS_NETWORK);
            case ERROR -> new BusinessException(ExceptionType.WIFI_VALIDATION_ERROR);
            default -> new BusinessException(ExceptionType.WIFI_INVALID_FORMAT);
        };
    }
    public void verifyCampusWifiConnection(StudyStartRequest request, Long userId) {
        WiFiValidationResult validationResult = wifiValidationService.validateCampusWiFi(
                request.gatewayIp(), request.clientIp()
        );

        if (!validationResult.isValid()) {
            log.warn("Wi-Fi validation failed for user {}: {}", userId, validationResult.getMessage());
            throw mapWiFiValidationException(validationResult);
        }
    }
    public StudySession makeStudySession(Long userId){
        // 현재 진행 중인 세션(STARTED 상태)만 체크
        studySessionRepository.findByUser_IdAndStatus(userId, StudyStatus.STARTED)
                .ifPresent(session -> {
                    throw new BusinessException(ExceptionType.ALREADY_STUDY_SESSION);
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        StudySession newStudySession = new StudySession();
        LocalDateTime startTime = LocalDateTime.now();
        newStudySession.startStudySession(startTime, user);
        return studySessionRepository.save(newStudySession);
    }

    @Transactional
    public List<User> endExpiredMaxFocusSessions() {
        int maxFocusHours = studyProperties.getMaxFocusHours();
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(maxFocusHours);

        List<StudySession> expiredSessions = studySessionRepository.findAllByStatusAndStartTimeBefore(
                StudyStatus.STARTED, cutoffTime
        );

        List<User> usersToNotify = new ArrayList<>();
        for (StudySession expiredSession : expiredSessions) {
            expiredSession.endMaxFocusStudySession(maxFocusHours);
            usersToNotify.add(expiredSession.getUser());
        }
        return usersToNotify;
    }
}
