package com.gpt.geumpumtabackend.fcm.outbox.retry;

public enum NotificationSendFailureType {
    // FCM 쿼터 요청량 초과
    QUOTA_EXCEEDED,
    // 일시 장애
    TRANSIENT,
    // 인증 설정
    PROVIDER_CONFIG,
    // 토큰 문제
    INVALID_TOKEN,
    UNKNOWN
}
