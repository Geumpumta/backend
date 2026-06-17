package com.gpt.geumpumtabackend.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationRetryPolicy {

    private final NotificationOutboxErrorClassifier errorClassifier;
    private final NotificationOutboxProperties properties;

    public NotificationRetryDecision decide(
            FirebaseMessagingException exception,
            int currentRetryCount,
            LocalDateTime now
    ) {
        String errorCode = exception.getMessagingErrorCode() == null ? "UNKNOWN" : exception.getMessagingErrorCode().name();

        String errorMessage = exception.getMessage();

        if(currentRetryCount >= properties.getRetry().getMaxRetry()) {
            return NotificationRetryDecision.dead(errorCode, errorMessage);
        }

        NotificationSendFailureType failureType =
                errorClassifier.classify(exception.getMessagingErrorCode());
        return switch (failureType) {
            case QUOTA_EXCEEDED -> NotificationRetryDecision.retry(
                    errorCode,
                    errorMessage,
                    now.plusSeconds(properties.getRetry().getQuotaDelaySeconds())
            );
            case TRANSIENT, UNKNOWN -> NotificationRetryDecision.retry(
                    errorCode,
                    errorMessage,
                    now.plusSeconds(properties.getRetry().getFirebaseErrorDelaySeconds())
            );
            case PROVIDER_CONFIG -> NotificationRetryDecision.retry(
                    errorCode,
                    errorMessage,
                    now.plusSeconds(properties.getRetry().getProviderConfigDelaySeconds())
            );
            case INVALID_TOKEN -> NotificationRetryDecision.dead(errorCode, errorMessage);
        };
    }
    public NotificationRetryDecision decideUnexpected(
            Exception exception,
            int currentRetryCount,
            LocalDateTime now
    ) {
        String errorCode = "FCM_UNEXPECTED_ERROR";
        String errorMessage = exception.getMessage();

        if (currentRetryCount >= properties.getRetry().getMaxRetry()) {
            return NotificationRetryDecision.dead(errorCode, errorMessage);
        }

        return NotificationRetryDecision.retry(
                errorCode,
                errorMessage,
                now.plusSeconds(properties.getRetry().getUnexpectedErrorDelaySeconds())
        );
    }
}
