package com.gpt.geumpumtabackend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "학습 세션 시작 요청")
public record StudyStartRequest(
        @Schema(description = "학습 시작 시간", example = "2024-01-15T09:00:00")
        @NotNull(message = "startTime은 필수입니다")
        LocalDateTime startTime,
        
        @Schema(description = "캠퍼스 네트워크 게이트웨이 IP 주소", example = "172.30.64.1")
        @NotBlank(message = "Gateway IP는 필수입니다")
        String gatewayIp
){
}


