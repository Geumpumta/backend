package com.gpt.geumpumtabackend.fcm.service;

import com.gpt.geumpumtabackend.token.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserSessionService userSessionService;

    @Transactional
    public void registerFcmToken(Long userId, String sessionId, String fcmToken) {
        userSessionService.registerFcmToken(userId, sessionId, fcmToken);
    }

    @Transactional
    public void removeFcmToken(Long userId, String sessionId) {
        userSessionService.removeFcmToken(userId, sessionId);
    }
}
