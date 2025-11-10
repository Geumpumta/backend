package com.gpt.geumpumtabackend.user.api;




import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "사용자 API", description = "사용자 관련 API")
public interface UserApi {

    @Operation(
            summary =  "회원가입 완료를 위한 추가 정보 입력 api",
            description = "GUEST 권한을 가진 사용자는 회원가입 완료를 위해 학번과 학부를 입력합니다." +
                    "사용자의 권한이 USER로 변경되고 accessToken과 refreshToken을 재발급받습니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = TokenResponse.class,
                    description = "회원가입 완료 및 accessToken과 refreshToken 재발급 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping("/complete-registration")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<TokenResponse>> completeRegistration(
            @RequestBody @Valid CompleteRegistrationRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "사용자의 정보를 반환하는 api",
            description = "USER, ADMIN 권한을 가진 사용자는 자신의 정보를 확인합니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = UserProfileResponse.class,
                    description = "사용자 정보 반환 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/profile")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<UserProfileResponse>> getMyProfile(
            @Parameter(hidden = true) Long userId
    );
}
