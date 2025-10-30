package com.gpt.geumpumtabackend.study.dto.response;

import com.gpt.geumpumtabackend.study.domain.StudySession;

public record StudyStartResponse(Long studySessionId) {

    public static StudyStartResponse fromEntity(StudySession studySession) {
        return new StudyStartResponse(studySession.getId());
    }
}
