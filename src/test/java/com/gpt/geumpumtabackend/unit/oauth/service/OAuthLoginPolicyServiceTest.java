package com.gpt.geumpumtabackend.unit.oauth.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtProperties;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.service.OAuthLoginPolicyService;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginPolicyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtHandler jwtHandler;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserSessionService userSessionService;

    private OAuthLoginPolicyService oAuthLoginPolicyService;

    @BeforeEach
    void setUp() {
        oAuthLoginPolicyService = new OAuthLoginPolicyService(
                userRepository,
                jwtHandler,
                jwtProperties,
                userSessionService
        );
    }

    @Test
    void 로그인하면_기존_세션을_교체하고_토큰을_발급한다() {
        Long userId = 1L;
        JwtUserClaim claim = new JwtUserClaim(userId, UserRole.USER, false);
        UserSession userSession = UserSession.builder()
                .userId(userId)
                .sessionId("session-id")
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        Token issuedToken = Token.builder()
                .accessToken("new-access")
                .refreshToken("refresh-token")
                .build();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(mock(User.class)));
        when(jwtProperties.getRefreshTokenExpireIn()).thenReturn(3600);
        when(userSessionService.createNewSession(userId, 3600)).thenReturn(userSession);
        when(jwtHandler.createTokens(any(JwtUserClaim.class), eq("refresh-token"))).thenReturn(issuedToken);

        Token result = oAuthLoginPolicyService.issueTokenReplacingActiveSession(claim);

        assertThat(result).isEqualTo(issuedToken);
        verify(userSessionService).createNewSession(userId, 3600);
        verify(jwtHandler).createTokens(argThat(sessionClaim ->
                sessionClaim.userId().equals(userId)
                        && sessionClaim.sessionId().equals("session-id")
                        && sessionClaim.role().equals(UserRole.USER)
                        && !sessionClaim.withdrawn()
        ), eq("refresh-token"));
    }

    @Test
    void 사용자_락_대상_없으면_예외를_던진다() {
        Long userId = 1L;
        JwtUserClaim claim = new JwtUserClaim(userId, UserRole.USER, false);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthLoginPolicyService.issueTokenReplacingActiveSession(claim))
                .isInstanceOf(BusinessException.class);

        verify(userSessionService, never()).createNewSession(anyLong(), anyLong());
        verify(jwtHandler, never()).createTokens(any(), anyString());
    }
}
