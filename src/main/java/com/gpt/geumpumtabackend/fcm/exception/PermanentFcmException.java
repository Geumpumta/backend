package com.gpt.geumpumtabackend.fcm.exception;

import com.google.firebase.messaging.FirebaseMessagingException;

public class PermanentFcmException extends RuntimeException {
    public PermanentFcmException(FirebaseMessagingException cause) {
        super(cause);
    }
}
