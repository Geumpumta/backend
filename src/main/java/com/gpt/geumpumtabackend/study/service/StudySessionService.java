package com.gpt.geumpumtabackend.study.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.wifi.IpUtil;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.dto.request.HeartBeatRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudySessionResponse;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final CampusWiFiValidationService wifiValidationService;

    /*
    메인 홈
     */
    public StudySessionResponse getTodayStudySession(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Long totalStudySession = studySessionRepository.sumCompletedStudySessionByUserId(userId, startOfDay, now);
        return StudySessionResponse.of(totalStudySession);
    }

    /*
    공부 시작
     */
    @Transactional
    public StudyStartResponse startStudySession(StudyStartRequest request, Long userId, HttpServletRequest httpServletRequest) {
        // Wi-Fi 검증
        WiFiValidationResult validationResult = wifiValidationService.validateFromCache(
            request.ssid(), request.bssid(), httpServletRequest
        );
        
        if (!validationResult.isValid()) {
            log.warn("Wi-Fi validation failed for user {}: {}", userId, validationResult.getMessage());
            throw mapWiFiValidationException(validationResult);
        }
        
        // 검증 성공 시 학습 세션 시작
        StudySession studySession = new StudySession();
        User user = userRepository.findById(userId)
                        .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        studySession.startStudySession(request.startTime(), user);
        
        StudySession savedSession = studySessionRepository.save(studySession);
        return StudyStartResponse.fromEntity(savedSession);
    }

    /*
    공부 종료
     */
    @Transactional
    public void endStudySession(StudyEndRequest request, Long userId) {
        StudySession studysession = studySessionRepository.findByIdAndUser_Id(request.studySessionId(), userId)
                .orElseThrow(()->new BusinessException(ExceptionType.STUDY_SESSION_NOT_FOUND));
        studysession.endStudySession(request.endTime());
    }


    /*
    하트비트 처리
     */
    @Transactional
    public void updateHeartBeat(HeartBeatRequest heartBeatRequest, Long userId, HttpServletRequest httpServletRequest) {
        Long sessionId = heartBeatRequest.sessionId();

        // Wi-Fi 검증 (캐시 우선 사용)
        WiFiValidationResult validationResult = wifiValidationService.validateFromCache(
            heartBeatRequest.ssid(), heartBeatRequest.bssid(), httpServletRequest
        );
        
        if (!validationResult.isValid()) {
            log.warn("Heartbeat Wi-Fi validation failed for user {}, session {}: {}", 
                userId, sessionId, validationResult.getMessage());
            throw mapWiFiValidationException(validationResult);
        }
        
        // 유효하면 해당 세션의 lastHeartBeatAt 시간을 now()로 갱신한다.
        StudySession studySession = studySessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(()->new BusinessException(ExceptionType.STUDY_SESSION_NOT_FOUND));
        studySession.updateHeartBeatAt(LocalDateTime.now());
    }

    private BusinessException mapWiFiValidationException(WiFiValidationResult result) {
        return switch (result.getStatus()) {
            case INVALID -> new BusinessException(ExceptionType.WIFI_NOT_CAMPUS_NETWORK);
            case ERROR -> new BusinessException(ExceptionType.WIFI_VALIDATION_ERROR);
            default -> new BusinessException(ExceptionType.WIFI_INVALID_FORMAT);
        };
    }
}
