package com.gpt.geumpumtabackend.token.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.domain.UserSessionStatus;
import com.gpt.geumpumtabackend.token.repository.UserSessionRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSessionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserSessionRepository userSessionRepository;

    @Transactional
    public UserSession createNewSession(Long userId, long refreshTokenExpireIn) {
        userSessionRepository.revokeActiveSessionsByUserId(userId);

        UserSession userSession = UserSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .refreshToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now(KST).plusSeconds(refreshTokenExpireIn))
                .build();

        return userSessionRepository.save(userSession);
    }

    @Transactional
    public UserSession validateActiveSession(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ExceptionType.SESSION_INVALID);
        }

        UserSession userSession = userSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ExceptionType.SESSION_INVALID));

        LocalDateTime now = LocalDateTime.now(KST);
        if (!userSession.getUserId().equals(userId) || !userSession.isActive() || userSession.isExpired(now)) {
            throw new BusinessException(ExceptionType.SESSION_INVALID);
        }

        userSession.touchLastSeen();
        return userSession;
    }

    @Transactional
    public UserSession validateRefreshToken(Long userId, String sessionId, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ExceptionType.REFRESH_TOKEN_NOT_EXIST);
        }

        UserSession userSession = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ExceptionType.REFRESH_TOKEN_NOT_EXIST));

        if (!userSession.getSessionId().equals(sessionId)) {
            throw new BusinessException(ExceptionType.TOKEN_NOT_MATCHED);
        }

        return validateActiveSession(userId, sessionId);
    }

    @Transactional
    public void logout(Long userId, String sessionId) {
        UserSession userSession = validateActiveSession(userId, sessionId);
        userSession.revoke();
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        userSessionRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void registerFcmToken(Long userId, String sessionId, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new BusinessException(ExceptionType.FCM_INVALID_TOKEN);
        }

        userSessionRepository.findByFcmToken(fcmToken)
                .ifPresent(UserSession::clearFcmToken);

        UserSession userSession = validateActiveSession(userId, sessionId);
        userSession.updateFcmToken(fcmToken);
    }

    @Transactional
    public void removeFcmToken(Long userId, String sessionId) {
        UserSession userSession = validateActiveSession(userId, sessionId);
        userSession.clearFcmToken();
    }

    public UserSession findActiveSession(Long userId) {
        UserSession userSession = userSessionRepository.findByUserIdAndStatus(userId, UserSessionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ExceptionType.SESSION_INVALID));

        LocalDateTime now = LocalDateTime.now(KST);
        if (userSession.isExpired(now)) {
            throw new BusinessException(ExceptionType.SESSION_INVALID);
        }
        return userSession;
    }

    @Transactional
    public void clearFcmToken(String fcmToken) {
        userSessionRepository.findByFcmToken(fcmToken)
                .ifPresent(UserSession::clearFcmToken);
    }
}
