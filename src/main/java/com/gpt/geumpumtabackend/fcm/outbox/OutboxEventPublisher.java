package com.gpt.geumpumtabackend.fcm.outbox;

import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    public void publish(NotificationOutbox outbox) {

    }
}
