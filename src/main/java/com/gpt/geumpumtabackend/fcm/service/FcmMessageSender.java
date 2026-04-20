package com.gpt.geumpumtabackend.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.fcm.exception.FcmPermanentException;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmMessageSender {

    private static final Set<MessagingErrorCode> PERMANENT_ERROR_CODES = Set.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH,
            MessagingErrorCode.THIRD_PARTY_AUTH_ERROR
    );

    private final UserRepository userRepository;

    public void send(FcmMessageDto messageDto) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle(messageDto.getTitle())
                .setBody(messageDto.getBody())
                .setImage(messageDto.getImageUrl())
                .build();

        Message.Builder messageBuilder = Message.builder()
                .setToken(messageDto.getToken())
                .setNotification(notification);

        if (messageDto.getData() != null && !messageDto.getData().isEmpty()) {
            messageBuilder.putAllData(messageDto.getData());
        }

        try {
            FirebaseMessaging.getInstance().send(messageBuilder.build());
        } catch (FirebaseMessagingException e) {
            handleSendFailure(e, messageDto.getToken());
        }
    }

    private void handleSendFailure(FirebaseMessagingException e, String token)
            throws FirebaseMessagingException {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        // 일시 오류 → 그대로 throw 하여 호출자가 outbox(PENDING) 에 기록하도록 위임
        if (errorCode == null || !PERMANENT_ERROR_CODES.contains(errorCode)) {
            throw e;
        }

        // UNREGISTERED → 토큰 제거로 자가치유, DEAD_LETTER 기록 불필요
        if (errorCode == MessagingErrorCode.UNREGISTERED) {
            log.warn("FCM token unregistered, clearing token: {}", token);
            userRepository.findByFcmToken(token)
                    .ifPresent(User::clearFcmToken);
            return;
        }

        // 그 외 영구 오류 → 운영 분석이 필요하므로 DEAD_LETTER 로 기록하도록 전용 예외 전파
        log.warn("FCM permanent error [{}] for token {}: {}", errorCode, token, e.getMessage());
        throw new FcmPermanentException(errorCode, e.getMessage(), e);
    }
}
