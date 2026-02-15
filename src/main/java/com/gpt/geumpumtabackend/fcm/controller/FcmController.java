package com.gpt.geumpumtabackend.fcm.controller;

import com.gpt.geumpumtabackend.fcm.api.FcmApi;
import com.gpt.geumpumtabackend.fcm.dto.request.FcmTokenRequest;
import com.gpt.geumpumtabackend.fcm.service.FcmService;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController implements FcmApi {

    private final FcmService fcmService;

    @Override
    public ResponseEntity<ResponseBody<Void>> registerFcmToken(FcmTokenRequest request, Long userId) {
        fcmService.registerFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    @Override
    public ResponseEntity<ResponseBody<Void>> removeFcmToken(Long userId) {
        fcmService.removeFcmToken(userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }
}
