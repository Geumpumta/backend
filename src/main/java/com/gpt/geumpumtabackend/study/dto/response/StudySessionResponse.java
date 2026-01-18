package com.gpt.geumpumtabackend.study.dto.response;

public record StudySessionResponse(Long totalStudySession, boolean isStudying) {

    public static StudySessionResponse of(Long totalStudySession, boolean isStudying) {
        return new StudySessionResponse(totalStudySession, isStudying);
    }
}
