package com.gpt.geumpumtabackend.token.service;



import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtHandler jwtHandler;
    private final UserSessionService userSessionService;

    @Transactional
    public TokenResponse refresh(Token token) {
        JwtUserClaim jwtUserClaim = jwtHandler.getClaims(token.getAccessToken())
                .orElseThrow(() -> new BusinessException(ExceptionType.JWT_INVALID)); // invalid token

        UserSession userSession = userSessionService.validateRefreshToken(
                jwtUserClaim.userId(),
                jwtUserClaim.sessionId(),
                token.getRefreshToken()
        );
        JwtUserClaim sessionClaim = JwtUserClaim.create(
                jwtUserClaim.userId(),
                userSession.getSessionId(),
                jwtUserClaim.role(),
                jwtUserClaim.withdrawn()
        );
        Token tokenResponse = jwtHandler.createTokens(sessionClaim, userSession.getRefreshToken());
        return new TokenResponse(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
    }


}
