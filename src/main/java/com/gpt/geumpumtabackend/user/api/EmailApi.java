package com.gpt.geumpumtabackend.user.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeRequest;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.response.EmailCodeVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "이메일 API", description = "이메일 관련 API")
public interface EmailApi {

    @Operation(
            summary =  "학교 이메일 인증코드 발송 pi",
            description = "GUEST 권한을 가진 사용자는 학교 이메일 인증을 위해 인증코드 발송을 요청합니다."

    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    description = "인증코드 발송 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.CANT_SEND_MAIL)
            }
    )
    @PostMapping("/request-code")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<Void>> requestCode(
            @RequestBody @Valid EmailCodeRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "학교 이메일 인증코드 검증 api",
            description = "GUEST 권한을 가진 사용자는 학교 이메일 인증 코드를 검증합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = EmailCodeVerifyResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = EmailCodeVerifyResponse.class,
                    description = "인증코드 검증 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping("/verify-code")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<EmailCodeVerifyResponse>> verifyCode(
            @RequestBody @Valid EmailCodeVerifyRequest request,
            @Parameter(hidden = true) Long userId
    );
}
