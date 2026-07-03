package com.gpt.geumpumtabackend.fcm.outbox;

import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryDecision;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryDecisionType;
import com.gpt.geumpumtabackend.fcm.repository.NotificationOutboxRepository;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationOutboxCommandService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final UserSessionService userSessionService;

    public void createMaxFocusOutbox(Long studySessionId, Long userId, int maxFocusHours) {
        String eventKey = "max-focus:" + studySessionId;

        if(notificationOutboxRepository.existsByEventKey(eventKey)) {
            return;
        }
        UserSession activeSession;
        try {
            activeSession = userSessionService.findActiveSession(userId);
        } catch(BusinessException e) {
            return;
        }

        String fcmToken = activeSession.getFcmToken();
        if(fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        NotificationOutbox outbox = NotificationOutbox.createMaxFocusNotification(studySessionId, userId, fcmToken, maxFocusHours);
        notificationOutboxRepository.save(outbox);
    }

    public Optional<NotificationOutbox> markProcessing(Long outboxId, String workerId, LocalDateTime now) {
        return notificationOutboxRepository.findByIdForUpdate(outboxId)
                .filter(outbox -> outbox.isDue(now))
                .map(outbox -> {
                    outbox.markProcessing(workerId,now);
                    return outbox;
                });
    }

    public void deleteAfterSendSuccess(Long outboxId) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        notificationOutboxRepository.delete(outbox);
    }

    public void applyRetryDecision(Long outboxId, NotificationRetryDecision decision) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        if(decision.type() == NotificationRetryDecisionType.DEAD) {
            outbox.markDead(decision.errorCode(), decision.errorMessage());
            return;
        }
        outbox.scheduleRetry(decision.errorCode(), decision.errorMessage(), decision.nextRetryAt());
    }
    public void markDead(Long outboxId, String errorCode, String errorMessage) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();

        outbox.markDead(errorCode, errorMessage);
    }
    public void markRetryScheduled(
            Long outboxId,
            String errorCode,
            String errorMessage,
            LocalDateTime nextRetryAt
    ) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        outbox.scheduleRetry(errorCode, errorMessage, nextRetryAt);
    }
    public void markCancelled(Long outboxId, String reason) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        outbox.markCancelled(reason);
    }
}
