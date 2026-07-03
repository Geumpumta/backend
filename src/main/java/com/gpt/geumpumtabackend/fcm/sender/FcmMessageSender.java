package com.gpt.geumpumtabackend.fcm.sender;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.gpt.geumpumtabackend.fcm.dto.FcmMessageDto;
import org.springframework.stereotype.Component;

@Component
public class FcmMessageSender {

    public String sendOnce(FcmMessageDto messageDto) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle(messageDto.getTitle())
                .setBody(messageDto.getBody())
                .setImage(messageDto.getImageUrl())
                .build();

        Message.Builder messageBuilder = Message.builder()
                .setToken(messageDto.getToken())
                .setNotification(notification);

        if (messageDto.getData() != null && !messageDto.getData().isEmpty()) {
            messageBuilder.putAllData(messageDto.getData());
        }


        return FirebaseMessaging.getInstance().send(messageBuilder.build());
    }
}
