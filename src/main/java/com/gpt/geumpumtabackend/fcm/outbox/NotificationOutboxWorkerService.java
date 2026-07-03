package com.gpt.geumpumtabackend.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutboxStatus;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryDecision;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryPolicy;
import com.gpt.geumpumtabackend.fcm.repository.NotificationOutboxRepository;
import com.gpt.geumpumtabackend.fcm.sender.FcmSendGuard;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationOutboxWorkerService {


    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FcmSendGuard fcmSendGuard;
    private final NotificationOutboxCommandService notificationOutboxCommandService;
    private final NotificationOutboxProperties notificationOutboxProperties;
    private final NotificationRetryPolicy notificationRetryPolicy;

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
            fcmSendGuard.send(outbox);
        } catch (RequestNotPermitted e) {
            NotificationRetryDecision decision = notificationRetryPolicy.decideRateLimited(now);
            notificationOutboxCommandService.applyRetryDecision(outboxId, decision);
            return;
        } catch(CallNotPermittedException e) {
            NotificationRetryDecision decision = notificationRetryPolicy.decideCircuitOpen(now);
            notificationOutboxCommandService.applyRetryDecision(outboxId, decision);
            return;
        } catch (FirebaseMessagingException e) {
            NotificationRetryDecision decision = notificationRetryPolicy.decide(
                    e,
                    outbox.getRetryCount(),
                    now
            );
            notificationOutboxCommandService.applyRetryDecision(outboxId, decision);
            return;
        } catch(Exception e) {
            NotificationRetryDecision decision = notificationRetryPolicy.decideUnexpected(
                    e,
                    outbox.getRetryCount(),
                    LocalDateTime.now()
            );
            notificationOutboxCommandService.applyRetryDecision(outboxId, decision);
            return;
        }
        notificationOutboxCommandService.deleteAfterSendSuccess(outboxId);
    }

}
