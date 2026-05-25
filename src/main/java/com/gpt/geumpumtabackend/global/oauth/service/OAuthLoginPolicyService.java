package com.gpt.geumpumtabackend.global.oauth.service;

import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtProperties;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginPolicyService {

    private final UserRepository userRepository;
    private final JwtHandler jwtHandler;
    private final JwtProperties jwtProperties;
    private final UserSessionService userSessionService;

    @Transactional
    public Token issueTokenReplacingActiveSession(JwtUserClaim jwtUserClaim) {
        userRepository.findByIdForUpdate(jwtUserClaim.userId())
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        UserSession userSession = userSessionService.createNewSession(
                jwtUserClaim.userId(),
                jwtProperties.getRefreshTokenExpireIn()
        );
        JwtUserClaim sessionClaim = JwtUserClaim.create(
                jwtUserClaim.userId(),
                userSession.getSessionId(),
                jwtUserClaim.role(),
                jwtUserClaim.withdrawn()
        );

        return jwtHandler.createTokens(sessionClaim, userSession.getRefreshToken());
    }
}
