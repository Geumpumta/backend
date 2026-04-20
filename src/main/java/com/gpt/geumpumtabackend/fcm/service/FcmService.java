package com.gpt.geumpumtabackend.fcm.service;

import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.fcm.exception.FcmPermanentException;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final FcmMessageSender fcmMessageSender;
    private final FcmOutboxService fcmOutboxService;

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

    public void sendMaxFocusNotification(Long userId, int hours) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Max focus notification skipped — user not found. userId={}", userId);
            return;
        }
        String token = user.getFcmToken();
        if (token == null || token.isBlank()) {
            return;
        }

        FcmMessageDto dto = buildMaxFocusDto(token, hours);
        try {
            fcmMessageSender.send(dto);
        } catch (FcmPermanentException e) {
            log.error("Max focus FCM permanent failure, saving to DEAD_LETTER. userId={}, errorCode={}",
                    userId, e.getErrorCode(), e);
            fcmOutboxService.saveDeadLetter(userId, dto, String.valueOf(e.getErrorCode()), e.getMessage());
        } catch (Exception e) {
            log.error("Max focus FCM transient failure, queuing to outbox(PENDING). userId={}", userId, e);
            fcmOutboxService.saveFailedNotification(userId, dto, e.getMessage());
        }
    }

    private FcmMessageDto buildMaxFocusDto(String token, int hours) {
        return FcmMessageDto.builder()
                .token(token)
                .title("최대 집중 시간 도달")
                .body(String.format("%d시간 동안 열심히 공부하셨습니다! 잠시 휴식을 취해보세요.", hours))
                .data(Map.of(
                        "type", "STUDY_SESSION_FORCE_ENDED",
                        "maxFocusHours", String.valueOf(hours)
                ))
                .build();
    }
}
