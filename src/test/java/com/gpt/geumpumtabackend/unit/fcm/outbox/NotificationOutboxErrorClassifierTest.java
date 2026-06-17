package com.gpt.geumpumtabackend.unit.fcm.outbox;

import com.google.firebase.messaging.MessagingErrorCode;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationOutboxErrorClassifier;
import com.gpt.geumpumtabackend.fcm.outbox.retry.NotificationSendFailureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationOutboxErrorClassifier")
class NotificationOutboxErrorClassifierTest {

    private final NotificationOutboxErrorClassifier classifier = new NotificationOutboxErrorClassifier();

    @Test
    @DisplayName("QUOTA_EXCEEDED는 쿼터 초과 실패로 분류한다")
    void quotaExceeded() {
        NotificationSendFailureType result = classifier.classify(MessagingErrorCode.QUOTA_EXCEEDED);

        assertThat(result).isEqualTo(NotificationSendFailureType.QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("UNAVAILABLE과 INTERNAL은 일시 실패로 분류한다")
    void transientErrors() {
        assertThat(classifier.classify(MessagingErrorCode.UNAVAILABLE))
                .isEqualTo(NotificationSendFailureType.TRANSIENT);
        assertThat(classifier.classify(MessagingErrorCode.INTERNAL))
                .isEqualTo(NotificationSendFailureType.TRANSIENT);
    }

    @Test
    @DisplayName("SENDER_ID_MISMATCH와 THIRD_PARTY_AUTH_ERROR는 provider 설정 실패로 분류한다")
    void providerConfigErrors() {
        assertThat(classifier.classify(MessagingErrorCode.SENDER_ID_MISMATCH))
                .isEqualTo(NotificationSendFailureType.PROVIDER_CONFIG);
        assertThat(classifier.classify(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR))
                .isEqualTo(NotificationSendFailureType.PROVIDER_CONFIG);
    }

    @Test
    @DisplayName("UNREGISTERED와 INVALID_ARGUMENT는 유효하지 않은 토큰 실패로 분류한다")
    void invalidTokenErrors() {
        assertThat(classifier.classify(MessagingErrorCode.UNREGISTERED))
                .isEqualTo(NotificationSendFailureType.INVALID_TOKEN);
        assertThat(classifier.classify(MessagingErrorCode.INVALID_ARGUMENT))
                .isEqualTo(NotificationSendFailureType.INVALID_TOKEN);
    }

    @Test
    @DisplayName("null error code는 UNKNOWN으로 분류한다")
    void nullErrorCode() {
        NotificationSendFailureType result = classifier.classify(null);

        assertThat(result).isEqualTo(NotificationSendFailureType.UNKNOWN);
    }
}
