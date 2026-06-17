package com.gpt.geumpumtabackend.fcm.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class NotificationOutbox extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    private Long userId;

    // 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationOutboxStatus status;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "event_key", nullable = false, unique = true, length = 128)
    private String eventKey;

    @Column(name = "fcm_token_snapshot", length = 512)
    private String fcmTokenSnapshot;

    @Column(length = 128)
    private String title;

    @Column(length = 512)
    private String body;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime lastAttemptAt;
    private String lockedBy;
    private String lastErrorMessage;
    private String lastErrorCode;

    public static NotificationOutbox createMaxFocusNotification(
            Long studySessionId,
            Long userId,
            String fcmTokenSnapshot,
            int maxFocusHours
    ) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.eventKey = "max-focus:" + studySessionId;
        outbox.userId = userId;
        outbox.fcmTokenSnapshot = fcmTokenSnapshot;
        outbox.title = "최대 집중 시간 도달";
        outbox.body = maxFocusHours + "시간 동안 공부했습니다. 잠시 휴식을 취해보세요.";
        outbox.status = NotificationOutboxStatus.PENDING;
        outbox.retryCount = 0;
        return outbox;
    }

    public boolean isDue(LocalDateTime now) {
        return (status == NotificationOutboxStatus.PENDING || status == NotificationOutboxStatus.RETRY_SCHEDULED)
                && (nextRetryAt == null || !nextRetryAt.isAfter(now));
    }

    public void markProcessing(String workerId, LocalDateTime now) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.lockedBy = workerId;
        this.lastAttemptAt = now;
    }

    public void scheduleRetry(String errorCode, String errorMessage, LocalDateTime nextRetryAt) {
        this.status = NotificationOutboxStatus.RETRY_SCHEDULED;
        this.retryCount++;
        this.nextRetryAt = nextRetryAt;
        this.lastErrorMessage = errorMessage;
        this.lockedBy = null;
        this.lastErrorCode = errorCode;
    }

    public void markDead(String errorCode, String errorMessage) {
        this.status = NotificationOutboxStatus.DEAD;
        this.lastErrorMessage = errorMessage;
        this.lockedBy = null;
        this.lastErrorCode = errorCode;
    }

}
