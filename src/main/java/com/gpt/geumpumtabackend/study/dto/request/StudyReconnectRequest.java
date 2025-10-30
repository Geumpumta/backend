package com.gpt.geumpumtabackend.study.dto.request;

import java.time.LocalDateTime;

public record StudyReconnectRequest(
        Long studySessionId,
        LocalDateTime disconnectedTime,
        LocalDateTime reconnectTime ) {
}
