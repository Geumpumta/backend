package com.gpt.geumpumtabackend.fcm.domain;

public enum NotificationOutboxStatus {

    PENDING,
    PROCESSING,
    RETRY_SCHEDULED,
    SENT,
    CANCELLED,
    DEAD

}
