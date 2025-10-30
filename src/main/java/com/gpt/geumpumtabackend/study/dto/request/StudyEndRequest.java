package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StudyEndRequest(
        @NotNull(message = "studySessionId는 필수입니다.")
        Long studySessionId,
        @NotNull(message = "endTime은 필수입니다.")
        LocalDateTime endTime) {
}
