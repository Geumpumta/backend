package com.gpt.geumpumtabackend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "학습 세션 하트비트 요청")
public record HeartBeatRequest(
        @Schema(description = "학습 세션 ID", example = "1")
        @NotNull(message = "sessionId는 필수입니다")
        Long sessionId,
        
        @Schema(description = "캠퍼스 네트워크 게이트웨이 IP 주소", example = "172.30.64.1")
        @NotBlank(message = "Gateway IP는 필수입니다")
        String gatewayIp,
        
        @Schema(description = "클라이언트 IP 주소", example = "192.168.1.100")
        @NotBlank(message = "Client IP는 필수입니다")
        String clientIp
) {

}
