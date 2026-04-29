package com.gpt.geumpumtabackend.study.dto.response;

import com.gpt.geumpumtabackend.study.domain.StudySession;

import java.time.LocalDateTime;

public record StudyStartResponse(Long studySessionId, LocalDateTime startTime) {

    public static StudyStartResponse fromEntity(StudySession studySession) {
        return new StudyStartResponse(studySession.getId(), studySession.getStartTime());
    }
}
