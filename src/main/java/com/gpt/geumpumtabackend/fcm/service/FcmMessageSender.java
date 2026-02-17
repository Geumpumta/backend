package com.gpt.geumpumtabackend.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    @Retryable(
            retryFor = FirebaseMessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
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

    @Recover
    public void sendRecover(FirebaseMessagingException e, FcmMessageDto messageDto) {
        log.error("FCM send failed after 3 retries for token {}", messageDto.getToken(), e);
        throw new BusinessException(ExceptionType.FCM_SEND_FAILED);
    }

    private void handleSendFailure(FirebaseMessagingException e, String token)
            throws FirebaseMessagingException {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == null || !PERMANENT_ERROR_CODES.contains(errorCode)) {
            throw e;
        }

        if (errorCode == MessagingErrorCode.UNREGISTERED) {
            log.warn("FCM token unregistered, clearing token: {}", token);
            userRepository.findByFcmToken(token)
                    .ifPresent(User::clearFcmToken);
            return;
        }

        log.warn("FCM permanent error [{}] for token {}: {}", errorCode, token, e.getMessage());
    }
}
