package com.gpt.geumpumtabackend.unit.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.gpt.geumpumtabackend.fcm.outbox.NotificationOutboxProperties;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationOutboxErrorClassifier;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryDecision;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryDecisionType;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationRetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("NotificationRetryPolicy")
class NotificationRetryPolicyTest {

    private NotificationRetryPolicy retryPolicy;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        NotificationOutboxProperties properties = new NotificationOutboxProperties();
        properties.getRetry().setMaxRetry(4);
        properties.getRetry().setFirebaseErrorDelaySeconds(10);
        properties.getRetry().setUnexpectedErrorDelaySeconds(30);
        properties.getRetry().setQuotaDelaySeconds(60);
        properties.getRetry().setProviderConfigDelaySeconds(300);
        properties.getRetry().setCircuitOpenDelaySeconds(30);
        properties.getRetry().setRateLimitedDelaySeconds(10);

        retryPolicy = new NotificationRetryPolicy(
                new NotificationOutboxErrorClassifier(),
                properties
        );
        now = LocalDateTime.of(2026, 6, 17, 12, 0);
    }

    @Test
    @DisplayName("QUOTA_EXCEEDED는 quota delay 이후 재시도한다")
    void quotaExceededRetriesWithQuotaDelay() {
        FirebaseMessagingException exception = firebaseException(
                MessagingErrorCode.QUOTA_EXCEEDED,
                "quota exceeded"
        );

        NotificationRetryDecision decision = retryPolicy.decide(exception, 0, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("QUOTA_EXCEEDED");
        assertThat(decision.errorMessage()).isEqualTo("quota exceeded");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(60));
    }

    @Test
    @DisplayName("UNAVAILABLE은 기본 Firebase 오류 delay 이후 재시도한다")
    void transientErrorRetriesWithFirebaseDelay() {
        FirebaseMessagingException exception = firebaseException(
                MessagingErrorCode.UNAVAILABLE,
                "temporarily unavailable"
        );

        NotificationRetryDecision decision = retryPolicy.decide(exception, 1, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("UNAVAILABLE");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    @DisplayName("THIRD_PARTY_AUTH_ERROR는 provider 설정 오류 delay 이후 재시도한다")
    void providerConfigErrorRetriesWithProviderConfigDelay() {
        FirebaseMessagingException exception = firebaseException(
                MessagingErrorCode.THIRD_PARTY_AUTH_ERROR,
                "auth error"
        );

        NotificationRetryDecision decision = retryPolicy.decide(exception, 0, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("THIRD_PARTY_AUTH_ERROR");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(300));
    }

    @Test
    @DisplayName("UNREGISTERED는 재시도하지 않고 DEAD 처리한다")
    void invalidTokenGoesDead() {
        FirebaseMessagingException exception = firebaseException(
                MessagingErrorCode.UNREGISTERED,
                "token unregistered"
        );

        NotificationRetryDecision decision = retryPolicy.decide(exception, 0, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.DEAD);
        assertThat(decision.errorCode()).isEqualTo("UNREGISTERED");
        assertThat(decision.nextRetryAt()).isNull();
    }

    @Test
    @DisplayName("maxRetry 이상이면 오류 유형과 관계없이 DEAD 처리한다")
    void maxRetryGoesDead() {
        FirebaseMessagingException exception = firebaseException(
                MessagingErrorCode.QUOTA_EXCEEDED,
                "quota exceeded"
        );

        NotificationRetryDecision decision = retryPolicy.decide(exception, 4, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.DEAD);
        assertThat(decision.errorCode()).isEqualTo("QUOTA_EXCEEDED");
        assertThat(decision.nextRetryAt()).isNull();
    }

    @Test
    @DisplayName("예상하지 못한 오류는 unexpected delay 이후 재시도한다")
    void unexpectedErrorRetriesWithUnexpectedDelay() {
        RuntimeException exception = new RuntimeException("unexpected");

        NotificationRetryDecision decision = retryPolicy.decideUnexpected(exception, 0, now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("FCM_UNEXPECTED_ERROR");
        assertThat(decision.errorMessage()).isEqualTo("unexpected");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(30));
    }

    @Test
    @DisplayName("CircuitBreaker OPEN은 FCM_CIRCUIT_OPEN으로 재시도 예약한다")
    void circuitOpenRetries() {
        NotificationRetryDecision decision = retryPolicy.decideCircuitOpen(now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("FCM_CIRCUIT_OPEN");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(30));
    }

    @Test
    @DisplayName("RateLimiter 거절은 FCM_RATE_LIMITED로 재시도 예약한다")
    void rateLimitedRetries() {
        NotificationRetryDecision decision = retryPolicy.decideRateLimited(now);

        assertThat(decision.type()).isEqualTo(NotificationRetryDecisionType.RETRY);
        assertThat(decision.errorCode()).isEqualTo("FCM_RATE_LIMITED");
        assertThat(decision.nextRetryAt()).isEqualTo(now.plusSeconds(10));
    }

    private FirebaseMessagingException firebaseException(
            MessagingErrorCode errorCode,
            String message
    ) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(errorCode);
        given(exception.getMessage()).willReturn(message);
        return exception;
    }
}
