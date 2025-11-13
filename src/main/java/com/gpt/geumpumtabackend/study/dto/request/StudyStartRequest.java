package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StudyStartRequest(
        @NotNull(message = "startTime은 필수입니다")
        LocalDateTime startTime,
        

        @NotNull(message = "Gateway IP는 필수입니다.")
        Integer gatewayIp,

        @NotNull(message = "IP 주소는 필수입니다.")
        Integer ipAddress,

        @NotBlank(message = "BSSID는 필수입니다")
        String bssid

){
}


