package com.gpt.geumpumtabackend.fcm.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.outbox")
@Getter
@Setter
public class NotificationOutboxProperties {

    private Worker worker = new Worker();
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;
        private long fixedDelayMs = 10_000;
        private long initialDelayMs = 5_000;
        private int batchSize = 100;
        private String workerId = "notification-worker";
    }
    @Getter
    @Setter
    public static class Retry {
        private int maxRetry = 4;
        private long firebaseErrorDelaySeconds = 10;
        private long unexpectedErrorDelaySeconds = 30;
        private long quotaDelaySeconds = 60;
        private long providerConfigDelaySeconds = 300;
    }
}
