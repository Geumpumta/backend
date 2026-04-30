package com.gpt.geumpumtabackend.study.dto.response;

import java.time.LocalDateTime;

public record StudySessionResponse(Long totalStudySession, boolean isStudying, LocalDateTime startTime) {

    public static StudySessionResponse of(Long totalStudySession, boolean isStudying, LocalDateTime startTime) {
        return new StudySessionResponse(totalStudySession, isStudying, startTime);
    }
}
