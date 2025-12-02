package com.gpt.geumpumtabackend.user.api;




import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.dto.request.NicknameVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.request.ProfileUpdateRequest;
import com.gpt.geumpumtabackend.user.dto.response.NicknameVerifyResponse;
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
import org.springframework.web.bind.annotation.*;

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

    @Operation(
            summary =  "닉네임 검증을 위한 api",
            description = "사용 가능한 닉네임인지 검증합니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = NicknameVerifyResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = NicknameVerifyResponse.class,
                    description = "닉네임 검증 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/nickname/verify")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<NicknameVerifyResponse>> verifyNickname(
            @RequestParam @Valid NicknameVerifyRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "프로필 수정을 위한 api",
            description = "프로필 수정을 진행합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(description = "프로필 수정 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping("/profile")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<Void>> updateProfile(
            @RequestBody @Valid ProfileUpdateRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "로그아웃을 위한 api",
            description = "로그아웃을 진행합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(description = "로그아웃 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @DeleteMapping("/logout")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<Void>> logout(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "회원탈퇴를 위한 api",
            description = "회원탈퇴를 진행합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(description = "회원탈퇴 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @DeleteMapping("/withdraw")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<Void>> withdrawCurrentUser(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "회원탈퇴한 계정 복구를 위한 api",
            description = "회원탈퇴한 계정을 복구합니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = TokenResponse.class,
                    description = "계정 복구 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping("/restore")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<TokenResponse>> restoreUser(
            @Parameter(hidden = true) Long userId
    );
}
