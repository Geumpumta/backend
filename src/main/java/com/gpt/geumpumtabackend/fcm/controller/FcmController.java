package com.gpt.geumpumtabackend.fcm.controller;

import com.gpt.geumpumtabackend.fcm.api.FcmApi;
import com.gpt.geumpumtabackend.fcm.dto.request.FcmTokenRequest;
import com.gpt.geumpumtabackend.fcm.service.FcmService;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController implements FcmApi {

    private final FcmService fcmService;

    @Override
    @PostMapping("/token")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<Void>> registerFcmToken(@RequestBody @Valid FcmTokenRequest request, Long userId) {
        fcmService.registerFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    @Override
    @DeleteMapping("/token")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<Void>> removeFcmToken(Long userId) {
        fcmService.removeFcmToken(userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }
}
