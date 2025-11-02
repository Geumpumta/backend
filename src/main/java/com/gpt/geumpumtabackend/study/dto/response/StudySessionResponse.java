package com.gpt.geumpumtabackend.study.dto.response;

public record StudySessionResponse(Long totalStudySession) {

    public static StudySessionResponse of(Long totalStudySession) {
        return new StudySessionResponse(totalStudySession);
    }
}
