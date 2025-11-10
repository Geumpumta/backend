package com.gpt.geumpumtabackend.wifi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@AllArgsConstructor
@Builder
public class WiFiValidationResult {
    
    private final boolean valid;        // 검증 성공 여부
    private final String message;       // 결과 메시지
    private final ValidationStatus status; // 상태 (VALID, INVALID, ERROR)
    

    public static WiFiValidationResult valid(String message) {
        return WiFiValidationResult.builder()
            .valid(true)
            .message(message)
            .status(ValidationStatus.VALID)
            .build();
    }
    

    public static WiFiValidationResult invalid(String message) {
        return WiFiValidationResult.builder()
            .valid(false)
            .message(message)
            .status(ValidationStatus.INVALID)
            .build();
    }
    

    public static WiFiValidationResult error(String message) {
        return WiFiValidationResult.builder()
            .valid(false)
            .message(message)
            .status(ValidationStatus.ERROR)
            .build();
    }
    

    public enum ValidationStatus {
        VALID,      // 유효함
        INVALID,    // 무효함 (SSID/BSSID/IP 문제)
        ERROR       // 시스템 오류
    }
}