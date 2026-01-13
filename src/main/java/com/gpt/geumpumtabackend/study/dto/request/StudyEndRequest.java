package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotNull;


public record StudyEndRequest(
        @NotNull(message = "studySessionId는 필수입니다.")
        Long studySessionId) {
}
