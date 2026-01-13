package com.gpt.geumpumtabackend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "학습 세션 시작 요청")
public record StudyStartRequest(

        @Schema(description = "캠퍼스 네트워크 게이트웨이 IP 주소", example = "172.30.64.1")
        @NotBlank(message = "Gateway IP는 필수입니다")
        String gatewayIp,
        
        @Schema(description = "클라이언트 IP 주소", example = "192.168.1.100")
        @NotBlank(message = "Client IP는 필수입니다")
        String clientIp
){
}


