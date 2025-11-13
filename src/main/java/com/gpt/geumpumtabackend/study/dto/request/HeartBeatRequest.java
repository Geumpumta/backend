package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HeartBeatRequest(
        @NotNull(message = "sessionId는 필수입니다")
        Long sessionId,
        
        @NotNull(message = "Gateway IP는 필수입니다")
        Integer gatewayIp,

        @NotNull(message = "IP 주소는 필수입니다")
        Integer ipAddress,

        @NotBlank(message = "BSSID는 필수입니다")
        String bssid

) {

}
