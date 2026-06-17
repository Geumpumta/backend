package com.gpt.geumpumtabackend.fcm.outbox.retry;

import com.google.firebase.messaging.MessagingErrorCode;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxErrorClassifier {

    public NotificationSendFailureType classify(MessagingErrorCode errorCode) {
        if(errorCode == null) {
            return NotificationSendFailureType.UNKNOWN;
        }
        return switch(errorCode) {
            case QUOTA_EXCEEDED -> NotificationSendFailureType.QUOTA_EXCEEDED;
            case UNAVAILABLE, INTERNAL -> NotificationSendFailureType.TRANSIENT;
            case SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR -> NotificationSendFailureType.PROVIDER_CONFIG;
            case UNREGISTERED, INVALID_ARGUMENT -> NotificationSendFailureType.INVALID_TOKEN;
            default -> NotificationSendFailureType.UNKNOWN;
        };
    }
}
