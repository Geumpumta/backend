package com.gpt.geumpumtabackend.fcm.exception;

import com.google.firebase.messaging.MessagingErrorCode;
import lombok.Getter;

@Getter
public class FcmPermanentException extends RuntimeException {

    private final MessagingErrorCode errorCode;

    public FcmPermanentException(MessagingErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
