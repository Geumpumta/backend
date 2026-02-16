package com.gpt.geumpumtabackend.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FcmService {

    private final UserRepository userRepository;

    @Transactional
    public void registerFcmToken(Long userId, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new BusinessException(ExceptionType.FCM_INVALID_TOKEN);
        }

        userRepository.findByFcmToken(fcmToken)
                .ifPresent(User::clearFcmToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        user.updateFcmToken(fcmToken);
    }

    @Transactional
    public void removeFcmToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        user.clearFcmToken();
    }

    @Retryable(
            retryFor = FirebaseMessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendMessage(FcmMessageDto messageDto) throws FirebaseMessagingException {
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

        FirebaseMessaging.getInstance().send(messageBuilder.build());
    }

    @Recover
    public void sendMessageRecover(FirebaseMessagingException e, FcmMessageDto messageDto) {
        log.error("FCM send failed after 3 retries for token {}", messageDto.getToken(), e);
        throw new BusinessException(ExceptionType.FCM_SEND_FAILED);
    }

    public void sendMaxFocusNotification(User user, int hours) {
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return;
        }

        FcmMessageDto messageDto = FcmMessageDto.builder()
                .token(user.getFcmToken())
                .title("최대 집중 시간 도달")
                .body(String.format("%d시간 동안 열심히 공부하셨습니다! 잠시 휴식을 취해보세요.", hours))
                .data(Map.of(
                        "type", "STUDY_SESSION_FORCE_ENDED",
                        "maxFocusHours", String.valueOf(hours)
                ))
                .build();
        try {
            sendMessage(messageDto);
        } catch (Exception e) {
            log.error("Failed to send max focus notification to user {}", user.getId(), e);
        }
    }
}
