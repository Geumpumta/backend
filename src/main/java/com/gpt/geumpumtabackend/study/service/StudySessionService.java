package com.gpt.geumpumtabackend.study.service;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.redis.RedisRankingSessionSnapshot;
import com.gpt.geumpumtabackend.rank.redis.RedisRealtimeRankingWriter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    // Redis 랭킹은 MySQL 커밋 이후 반영하는 파생 조회 모델이다. Redis 실패가 공부 시작/종료 요청을 실패시키면 안 된다.
    @Autowired(required = false)
    private RedisRealtimeRankingWriter redisRankingWriter;

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
        // DB 트랜잭션이 성공적으로 커밋된 뒤 Redis active ZSET에 반영한다.
        syncStartedSessionToRedisAfterCommit(savedSession);
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
        // DB에 종료 시간이 커밋된 뒤 Redis active 점수를 done 점수로 확정한다.
        syncEndedSessionToRedisAfterCommit(studysession);
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
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(maxFocusHours);

        List<StudySession> expiredSessions = studySessionRepository.findAllByStatusAndStartTimeBefore(
                StudyStatus.STARTED, cutoffTime
        );

        List<User> usersToNotify = new ArrayList<>();
        for (StudySession expiredSession : expiredSessions) {
            expiredSession.endMaxFocusStudySession(maxFocusHours);
            // 최대 집중 시간으로 자동 종료된 세션도 일반 종료와 동일하게 Redis 랭킹에 반영한다.
            syncEndedSessionToRedisAfterCommit(expiredSession);
            usersToNotify.add(expiredSession.getUser());
        }
        return usersToNotify;
    }

    /**
     * 공부 시작 정보를 Redis writer에 넘길 수 있는 스냅샷으로 만든다.
     * 학과가 없는 사용자는 학과 랭킹 계산이 불가능하므로 Redis 랭킹 반영 대상에서 제외한다.
     */
    private void syncStartedSessionToRedisAfterCommit(StudySession session) {
        if (redisRankingWriter == null || session == null) {
            return;
        }

        User user = session.getUser();
        if (session.getId() == null || user == null || user.getId() == null || user.getDepartment() == null) {
            return;
        }

        RedisRankingSessionSnapshot snapshot = RedisRankingSessionSnapshot.started(
                session.getId(),
                user.getId(),
                user.getDepartment(),
                session.getStartTime()
        );
        runAfterCommit(() -> redisRankingWriter.recordSessionStarted(snapshot));
    }

    /**
     * 공부 종료 정보를 Redis writer에 넘길 수 있는 스냅샷으로 만든다.
     * Redis 쓰기는 커밋 이후에 실행되어 DB 원본과 Redis 조회 모델의 순서를 지킨다.
     */
    private void syncEndedSessionToRedisAfterCommit(StudySession session) {
        if (redisRankingWriter == null || session == null) {
            return;
        }

        User user = session.getUser();
        if (session.getId() == null || user == null || user.getId() == null || user.getDepartment() == null
                || session.getEndTime() == null || session.getTotalMillis() == null) {
            return;
        }

        RedisRankingSessionSnapshot snapshot = RedisRankingSessionSnapshot.ended(
                session.getId(),
                user.getId(),
                user.getDepartment(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalMillis()
        );
        runAfterCommit(() -> redisRankingWriter.recordSessionEnded(snapshot));
    }

    /**
     * 현재 트랜잭션이 있으면 커밋 이후 실행하고, 트랜잭션이 없으면 즉시 실행한다.
     * Redis는 파생 조회 모델이므로 DB 커밋보다 먼저 쓰지 않는 것이 핵심이다.
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }

        action.run();
    }
}
