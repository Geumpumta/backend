package com.gpt.geumpumtabackend.unit.token.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.token.service.TokenService;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.unit.config.BaseUnitTest;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TokenService 단위 테스트")
class TokenServiceTest extends BaseUnitTest {

    @Mock
    private JwtHandler jwtHandler;

    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private TokenService tokenService;

    @Test
    @DisplayName("Refresh Token이 유효하면 새 Access Token을 발급한다")
    void 유효한_RefreshToken이면_AccessToken을_재발급한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        String refreshToken = "refresh-token";
        Token requestToken = Token.builder()
                .accessToken("expired-access-token")
                .refreshToken(refreshToken)
                .build();
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.USER, false);
        UserSession userSession = createUserSession(userId, sessionId, refreshToken);
        Token issuedToken = Token.builder()
                .accessToken("new-access-token")
                .refreshToken(refreshToken)
                .build();

        when(jwtHandler.getClaims(requestToken.getAccessToken())).thenReturn(Optional.of(claim));
        when(userSessionService.validateRefreshToken(userId, sessionId, refreshToken)).thenReturn(userSession);
        when(jwtHandler.createTokens(argThat(sessionClaim ->
                sessionClaim.userId().equals(userId)
                        && sessionClaim.sessionId().equals(sessionId)
                        && sessionClaim.role().equals(UserRole.USER)
                        && !sessionClaim.withdrawn()
        ), eq(refreshToken))).thenReturn(issuedToken);

        TokenResponse response = tokenService.refresh(requestToken);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        verify(userSessionService).validateRefreshToken(userId, sessionId, refreshToken);
    }

    @Test
    @DisplayName("Access Token claim 파싱에 실패하면 JWT_INVALID 예외가 발생한다")
    void AccessToken_claim_파싱_실패시_예외가_발생한다() {
        Token requestToken = Token.builder()
                .accessToken("invalid-access-token")
                .refreshToken("refresh-token")
                .build();

        when(jwtHandler.getClaims(requestToken.getAccessToken())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refresh(requestToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.JWT_INVALID);
        verify(userSessionService, never()).validateRefreshToken(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token이면 예외가 전파된다")
    void 존재하지_않는_RefreshToken이면_예외가_전파된다() {
        assertRefreshFailure(ExceptionType.REFRESH_TOKEN_NOT_EXIST);
    }

    @Test
    @DisplayName("Access Token의 sessionId와 Refresh Token의 세션이 다르면 예외가 전파된다")
    void sessionId가_일치하지_않으면_예외가_전파된다() {
        assertRefreshFailure(ExceptionType.TOKEN_NOT_MATCHED);
    }

    @Test
    @DisplayName("REVOKED 세션의 Refresh Token이면 예외가 전파된다")
    void REVOKED_세션의_RefreshToken이면_예외가_전파된다() {
        assertRefreshFailure(ExceptionType.SESSION_INVALID);
    }

    private void assertRefreshFailure(ExceptionType exceptionType) {
        Long userId = 1L;
        String sessionId = "session-id";
        String refreshToken = "refresh-token";
        Token requestToken = Token.builder()
                .accessToken("expired-access-token")
                .refreshToken(refreshToken)
                .build();
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.USER, false);

        when(jwtHandler.getClaims(requestToken.getAccessToken())).thenReturn(Optional.of(claim));
        when(userSessionService.validateRefreshToken(userId, sessionId, refreshToken))
                .thenThrow(new BusinessException(exceptionType));

        assertThatThrownBy(() -> tokenService.refresh(requestToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", exceptionType);
    }

    private UserSession createUserSession(Long userId, String sessionId, String refreshToken) {
        return UserSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
