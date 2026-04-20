package com.gpt.geumpumtabackend.fcm.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.fcm.domain.FcmNotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.FcmOutboxStatus;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import com.gpt.geumpumtabackend.fcm.repository.FcmOutboxRepository;
import com.gpt.geumpumtabackend.fcm.service.FcmMessageSender;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmOutboxRetryScheduler {

    private static final TypeReference<Map<String, String>> DATA_TYPE = new TypeReference<>() {};

    private final FcmOutboxRepository outboxRepository;
    private final FcmMessageSender fcmMessageSender;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedNotifications() {
        List<FcmNotificationOutbox> pending = outboxRepository
                .findAllByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                        FcmOutboxStatus.PENDING,
                        LocalDateTime.now()
                );
        if (pending.isEmpty()) {
            return;
        }
        log.info("[FCM_OUTBOX] retry batch start. size={}", pending.size());

        for (FcmNotificationOutbox entry : pending) {
            try {
                fcmMessageSender.send(buildDto(entry));
                entry.markSent();
            } catch (Exception e) {
                log.warn("[FCM_OUTBOX] retry failed. outboxId={}, retryCount={}",
                        entry.getId(), entry.getRetryCount(), e);
                entry.markRetryScheduled(e.getMessage());
            }
            // 건별 독립 트랜잭션으로 커밋하여 FCM HTTP 호출 중 DB 커넥션이 장시간 점유되지 않도록 한다.
            outboxRepository.save(entry);
        }
    }

    private FcmMessageDto buildDto(FcmNotificationOutbox entry) {
        Map<String, String> data = null;
        if (entry.getDataJson() != null && !entry.getDataJson().isBlank()) {
            try {
                data = objectMapper.readValue(entry.getDataJson(), DATA_TYPE);
            } catch (JsonProcessingException e) {
                log.warn("[FCM_OUTBOX] failed to deserialize dataJson. outboxId={}", entry.getId(), e);
            }
        }
        return FcmMessageDto.builder()
                .token(entry.getFcmToken())
                .title(entry.getTitle())
                .body(entry.getBody())
                .data(data)
                .build();
    }
}
