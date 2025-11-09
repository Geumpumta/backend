package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HeartBeatRequest(
        @NotNull(message = "sessionId는 필수입니다")
        Long sessionId,
        
        @NotBlank(message = "SSID는 필수입니다")
        String ssid,
        
        String bssid,  // 선택적 (null 가능)
        
        @NotBlank(message = "IP 주소는 필수입니다")
        String ipAddress
) {

}
