package com.gpt.geumpumtabackend.fcm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.fcm.domain.FcmNotificationOutbox;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.fcm.repository.FcmOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmOutboxService {

    private final FcmOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 일시 오류로 실패한 FCM 메시지를 Outbox(PENDING) 에 영속화한다.
     * 호출자 트랜잭션과 독립적으로 커밋되어야 하므로 REQUIRES_NEW 를 사용한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedNotification(Long userId, FcmMessageDto dto, String lastError) {
        String dataJson = serializeData(userId, dto);
        FcmNotificationOutbox outbox = FcmNotificationOutbox.ofPending(
                userId,
                dto.getToken(),
                dto.getTitle(),
                dto.getBody(),
                dataJson,
                lastError
        );
        outboxRepository.save(outbox);
    }

    /**
     * 영구 오류로 실패한 FCM 메시지를 Outbox(DEAD_LETTER) 에 영속화한다.
     * 재시도하지 않으며 운영 분석/수동 조치를 위한 기록 목적이다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveDeadLetter(Long userId, FcmMessageDto dto, String errorCode, String message) {
        String dataJson = serializeData(userId, dto);
        String lastError = String.format("[%s] %s", errorCode, message);
        FcmNotificationOutbox outbox = FcmNotificationOutbox.ofDeadLetter(
                userId,
                dto.getToken(),
                dto.getTitle(),
                dto.getBody(),
                dataJson,
                lastError
        );
        outboxRepository.save(outbox);
    }

    private String serializeData(Long userId, FcmMessageDto dto) {
        if (dto.getData() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dto.getData());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FCM data payload. userId={}", userId, e);
            return null;
        }
    }
}
