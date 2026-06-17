package com.gpt.geumpumtabackend.fcm.outbox;

import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.repository.NotificationOutboxRepository;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationOutboxCommandService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final UserSessionService userSessionService;
    private final NotificationOutboxProperties notificationOutboxProperties;

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

    public void deleteSent(Long outboxId) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        notificationOutboxRepository.delete(outbox);
    }

    public void markRetryOrDead(Long outboxId, String errorCode, String errorMessage, LocalDateTime nextRetryAt) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();
        if(outbox.getRetryCount() >= notificationOutboxProperties.getRetry().getMaxRetry()) {
            outbox.markDead(errorCode, errorMessage);
            return;
        }
        outbox.scheduleRetry(errorCode, errorMessage, nextRetryAt);
    }
    public void markDead(Long outboxId, String errorCode, String errorMessage) {
        NotificationOutbox outbox = notificationOutboxRepository.findByIdForUpdate(outboxId)
                .orElseThrow();

        outbox.markDead(errorCode, errorMessage);
    }
}
