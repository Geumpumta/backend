package com.gpt.geumpumtabackend.user.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeRequest;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.response.EmailCodeVerifyResponse;
import com.gpt.geumpumtabackend.user.service.EmailService;
import com.gpt.geumpumtabackend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final UserService userService;

    /**
     * 이메일 인증 코드 전송 api
     * @param request
     * @return
     */
    @PostMapping("/request-code")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<Void>> requestCode(
            @RequestBody @Valid EmailCodeRequest request,
            Long userId
    ){
        userService.saveSchoolEmail(userId, request);
        emailService.sendMail(request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    /**
     * 이메일 인증 코드 검증 api
     * @param request
     * @return
     */
    @PostMapping("/verify-code")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('GUEST')")
    public ResponseEntity<ResponseBody<EmailCodeVerifyResponse>> verifyCode(
            @RequestBody @Valid EmailCodeVerifyRequest request,
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(emailService.verifyCode(request, userId)));
    }
}
