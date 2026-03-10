package com.gpt.geumpumtabackend.fcm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 토큰 등록 요청")
public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Schema(description = "FCM 디바이스 토큰", example = "eXaMpLeToKeN123...")
        String fcmToken
) {
}
