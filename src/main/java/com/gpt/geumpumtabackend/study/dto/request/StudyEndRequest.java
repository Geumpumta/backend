package com.gpt.geumpumtabackend.study.dto.request;

import java.time.LocalDateTime;

public record StudyEndRequest(
        Long studySessionId,
        LocalDateTime endTime) {
}
