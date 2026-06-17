package com.gpt.geumpumtabackend.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutboxStatus;
import com.gpt.geumpumtabackend.fcm.repository.NotificationOutboxRepository;
import com.gpt.geumpumtabackend.fcm.service.FcmService;
import com.gpt.geumpumtabackend.study.config.StudyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationOutboxWorkerService {

    private static final int BATCH_SIZE = 100;

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FcmService fcmService;
    private final NotificationOutboxCommandService notificationOutboxCommandService;

    @Scheduled(
            fixedDelay=10,
            initialDelay=5,
            timeUnit = TimeUnit.SECONDS
    )
    public void publishPendingEvent() {
        List<NotificationOutbox> outboxes = notificationOutboxRepository.findDueOutboxes(
                List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.RETRY_SCHEDULED),
                LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)
        );
        for(NotificationOutbox outbox:outboxes) {
            processOne(outbox.getId());
        }
    }

    private void processOne(Long outboxId) {
        LocalDateTime now = LocalDateTime.now();
        String workerId = "notification-worker";

        Optional<NotificationOutbox> processingOutbox =
                notificationOutboxCommandService.markProcessing(outboxId, workerId, now);
        if(processingOutbox.isEmpty()) {
            return;
        }
        NotificationOutbox outbox = processingOutbox.get();
        try {
            String ProviderMessageId = fcmService.sendOutbox(outbox);
            notificationOutboxCommandService.markSent(outboxId, ProviderMessageId);
        } catch (FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode() == null ? "UNKNOWN" : e.getMessagingErrorCode().name();
            LocalDateTime nextRetryAt = now.plusSeconds(10);
            notificationOutboxCommandService.markRetryOrDead(outboxId, errorCode, e.getMessage(), nextRetryAt);
        } catch (Exception e) {
            notificationOutboxCommandService.markRetryOrDead(
                    outbox.getId(),
                    "FCM_UNEXPECTED_ERROR",
                    e.getMessage(),
                    LocalDateTime.now().plusSeconds(30)
            );
        }
    }
}
