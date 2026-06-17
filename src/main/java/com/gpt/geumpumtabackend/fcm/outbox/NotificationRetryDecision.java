package com.gpt.geumpumtabackend.fcm.outbox;

import java.time.LocalDateTime;

public record NotificationRetryDecision(
        NotificationRetryDecisionType type,
        String errorCode,
        String errorMessage,
        LocalDateTime nextRetryAt
) {
    public static NotificationRetryDecision retry(
            String errorCode,
            String errorMessage,
            LocalDateTime nextRetryAt
    ) {
        return new NotificationRetryDecision(NotificationRetryDecisionType.RETRY, errorCode, errorMessage, nextRetryAt);
    }

    public static NotificationRetryDecision dead(
            String errorCode,
            String errorMessage
    ) {
        return new NotificationRetryDecision(NotificationRetryDecisionType.DEAD, errorCode, errorMessage, null);
    }
}
