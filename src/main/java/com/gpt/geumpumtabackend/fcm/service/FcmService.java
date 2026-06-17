package com.gpt.geumpumtabackend.fcm.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
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

    public String sendOutbox(NotificationOutbox outbox) throws FirebaseMessagingException {
        FcmMessageDto messageDto = FcmMessageDto.builder()
                .token(outbox.getFcmTokenSnapshot())
                .title(outbox.getTitle())
                .body(outbox.getBody())
                .data(Map.of(
                        "type", "STUDY_SESSION_FORCE_ENDED",
                        "eventKey", outbox.getEventKey()
                ))
                .build();
        return fcmMessageSender.sendOnce(messageDto);
    }
}
