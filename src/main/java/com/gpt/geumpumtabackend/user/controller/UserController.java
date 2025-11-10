package com.gpt.geumpumtabackend.user.controller;


import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.user.api.UserApi;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.dto.response.UserProfileResponse;
import com.gpt.geumpumtabackend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    /**
     * 필수 정보 입력 후 토큰 재발급받는 api
     * @param request
     * @param userId
     * @return
     */
    @PostMapping("/complete-registration")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<TokenResponse>> completeRegistration(
            @RequestBody @Valid CompleteRegistrationRequest request,
            Long userId
    ){
        TokenResponse response = userService.completeRegistration(request, userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/profile")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<UserProfileResponse>> getMyProfile(
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                userService.getUserProfile(userId)
        ));
    }
}
