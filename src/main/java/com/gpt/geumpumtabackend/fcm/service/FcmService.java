package com.gpt.geumpumtabackend.fcm.service;

import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.user.domain.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FcmService {

    private final UserSessionService userSessionService;
    private final FcmMessageSender fcmMessageSender;

    @Transactional
    public void registerFcmToken(Long userId, String sessionId, String fcmToken) {
        userSessionService.registerFcmToken(userId, sessionId, fcmToken);
    }

    @Transactional
    public void removeFcmToken(Long userId, String sessionId) {
        userSessionService.removeFcmToken(userId, sessionId);
    }

    public void sendMaxFocusNotification(User user, int hours) {
        UserSession userSession;
        try {
            userSession = userSessionService.findActiveSession(user.getId());
        } catch (BusinessException e) {
            return;
        }
        String fcmToken = userSession.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        FcmMessageDto messageDto = FcmMessageDto.builder()
                .token(fcmToken)
                .title("최대 집중 시간 도달")
                .body(String.format("%d시간 동안 열심히 공부하셨습니다! 잠시 휴식을 취해보세요.", hours))
                .data(Map.of(
                        "type", "STUDY_SESSION_FORCE_ENDED",
                        "maxFocusHours", String.valueOf(hours)
                ))
                .build();
        try {
            fcmMessageSender.send(messageDto);
        } catch (Exception e) {
            log.error("Failed to send max focus notification to user {}", user.getId(), e);
        }
    }
}
