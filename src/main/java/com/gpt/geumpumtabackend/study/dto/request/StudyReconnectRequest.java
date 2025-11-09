package com.gpt.geumpumtabackend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StudyReconnectRequest(
        @NotNull(message = "studySessionId는 필수입니다.")
        Long studySessionId,
        
        @NotNull(message = "disconnectedTime은 필수입니다.")
        LocalDateTime disconnectedTime,
        
        @NotNull(message = "reconnectTime은 필수입니다.")
        LocalDateTime reconnectTime,
        
        @NotBlank(message = "SSID는 필수입니다")
        String ssid,
        
        String bssid,  // 선택적 (null 가능)
        
        @NotBlank(message = "IP 주소는 필수입니다")
        String ipAddress
) {
}
