package com.gpt.geumpumtabackend.fcm.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "fcm_notification_outbox",
        indexes = {
                @Index(name = "idx_outbox_status_next_retry", columnList = "status, next_retry_at")
        }
)
public class FcmNotificationOutbox extends BaseEntity {

    private static final int MAX_RETRY_COUNT = 5;
    private static final long MAX_BACKOFF_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 512)
    private String fcmToken;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(columnDefinition = "TEXT")
    private String dataJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FcmOutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(length = 1000)
    private String lastError;

    public static FcmNotificationOutbox ofPending(
            Long userId,
            String fcmToken,
            String title,
            String body,
            String dataJson,
            String lastError
    ) {
        FcmNotificationOutbox outbox = new FcmNotificationOutbox();
        outbox.userId = userId;
        outbox.fcmToken = fcmToken;
        outbox.title = title;
        outbox.body = body;
        outbox.dataJson = dataJson;
        outbox.status = FcmOutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.nextRetryAt = LocalDateTime.now();
        outbox.lastError = lastError;
        return outbox;
    }

    public static FcmNotificationOutbox ofDeadLetter(
            Long userId,
            String fcmToken,
            String title,
            String body,
            String dataJson,
            String lastError
    ) {
        FcmNotificationOutbox outbox = new FcmNotificationOutbox();
        outbox.userId = userId;
        outbox.fcmToken = fcmToken;
        outbox.title = title;
        outbox.body = body;
        outbox.dataJson = dataJson;
        outbox.status = FcmOutboxStatus.DEAD_LETTER;
        outbox.retryCount = 0;
        outbox.nextRetryAt = LocalDateTime.now();
        outbox.lastError = lastError;
        return outbox;
    }

    public void markSent() {
        this.status = FcmOutboxStatus.SENT;
    }

    public void markRetryScheduled(String error) {
        this.retryCount++;
        this.lastError = error;
        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = FcmOutboxStatus.DEAD_LETTER;
            this.nextRetryAt = LocalDateTime.now();
            return;
        }
        long backoffMinutes = Math.min(MAX_BACKOFF_MINUTES, (long) Math.pow(2, this.retryCount));
        this.nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
    }
}
