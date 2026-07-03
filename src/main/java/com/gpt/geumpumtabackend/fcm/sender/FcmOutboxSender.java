package com.gpt.geumpumtabackend.fcm.sender;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FcmOutboxSender {

    private final FcmMessageSender fcmMessageSender;

    public String send(NotificationOutbox outbox) throws FirebaseMessagingException {
        FcmMessageDto messageDto = FcmMessageDto.builder()
                .token(outbox.getFcmTokenSnapshot())
                .title(outbox.getTitle())
                .body(outbox.getBody())
                .data(Map.of(
                        "type", "STUDY_SESSION_FORCE_ENDED",
                        "eventKey", outbox.getEventKey()
                ))
                .build();
        return fcmMessageSender.sendOnce(messageDto);
    }
}
