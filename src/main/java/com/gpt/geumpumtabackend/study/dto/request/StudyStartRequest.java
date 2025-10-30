package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;


public record StudyStartRequest(
        @NotNull(message = "startTime은 필수입니다")
        LocalDateTime startTime){

}


