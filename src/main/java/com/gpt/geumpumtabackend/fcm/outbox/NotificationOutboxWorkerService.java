package com.gpt.geumpumtabackend.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutboxStatus;
import com.gpt.geumpumtabackend.fcm.repository.NotificationOutboxRepository;
import com.gpt.geumpumtabackend.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationOutboxWorkerService {


    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FcmService fcmService;
    private final NotificationOutboxCommandService notificationOutboxCommandService;
    private final NotificationOutboxProperties notificationOutboxProperties;

    @Scheduled(
            fixedDelayString = "${notification.outbox.worker.fixed-delay-ms:10000}",
            initialDelayString = "${notification.outbox.worker.initial-delay-ms:5000}"
    )
    public void publishPendingEvent() {
        if (!notificationOutboxProperties.getWorker().isEnabled()) {
            return;
        }

        List<NotificationOutbox> outboxes = notificationOutboxRepository.findDueOutboxes(
                List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.RETRY_SCHEDULED),
                LocalDateTime.now(),
                PageRequest.of(0, notificationOutboxProperties.getWorker().getBatchSize())
        );
        for(NotificationOutbox outbox:outboxes) {
            processOne(outbox.getId());
        }
    }

    private void processOne(Long outboxId) {
        LocalDateTime now = LocalDateTime.now();
        String workerId = notificationOutboxProperties.getWorker().getWorkerId();

        Optional<NotificationOutbox> processingOutbox =
                notificationOutboxCommandService.markProcessing(outboxId, workerId, now);
        if(processingOutbox.isEmpty()) {
            return;
        }
        NotificationOutbox outbox = processingOutbox.get();
        try {
            fcmService.sendOutbox(outbox);
        } catch (FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode() == null ? "UNKNOWN" : e.getMessagingErrorCode().name();
            LocalDateTime nextRetryAt = now.plusSeconds(
                    notificationOutboxProperties.getRetry().getFirebaseErrorDelaySeconds()
            );
            notificationOutboxCommandService.markRetryOrDead(outboxId, errorCode, e.getMessage(), nextRetryAt);
            return;
        } catch (Exception e) {
            notificationOutboxCommandService.markRetryOrDead(
                    outbox.getId(),
                    "FCM_UNEXPECTED_ERROR",
                    e.getMessage(),
                    LocalDateTime.now().plusSeconds(
                            notificationOutboxProperties.getRetry().getUnexpectedErrorDelaySeconds()
                    )
            );
            return;
        }
        notificationOutboxCommandService.deleteSent(outboxId);

    }
}
