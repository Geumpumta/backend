package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StudyReconnectRequest(
        @NotNull(message = "studySessionId는 필수입니다.")
        Long studySessionId,
        @NotNull(message = "disconnectedTime은 필수입니다.")
        LocalDateTime disconnectedTime,
        @NotNull(message = "reconnectTime은 필수입니다.")
        LocalDateTime reconnectTime ) {
}
