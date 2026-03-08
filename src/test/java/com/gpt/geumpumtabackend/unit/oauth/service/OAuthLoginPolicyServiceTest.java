package com.gpt.geumpumtabackend.unit.oauth.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.service.OAuthLoginPolicyService;
import com.gpt.geumpumtabackend.token.domain.RefreshToken;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthLoginPolicyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtHandler jwtHandler;

    private OAuthLoginPolicyService oAuthLoginPolicyService;

    @BeforeEach
    void setUp() {
        oAuthLoginPolicyService = new OAuthLoginPolicyService(userRepository, refreshTokenRepository, jwtHandler);
    }

    @Test
    void 활성_세션이_있으면_토큰_발급을_차단한다() {
        Long userId = 1L;
        JwtUserClaim claim = new JwtUserClaim(userId, UserRole.USER, false);
        RefreshToken activeToken = RefreshToken.builder()
                .userId(userId)
                .refreshToken("active-token")
                .times(3600L)
                .build();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(mock(User.class)));
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(activeToken));

        Optional<Token> result = oAuthLoginPolicyService.issueTokenIfNoActiveSession(claim);

        assertThat(result).isEmpty();
        verify(jwtHandler, never()).createTokens(any());
        verify(refreshTokenRepository, never()).deleteByUserId(userId);
    }

    @Test
    void 만료된_토큰만_있으면_삭제후_새_토큰을_발급한다() {
        Long userId = 1L;
        JwtUserClaim claim = new JwtUserClaim(userId, UserRole.USER, false);
        RefreshToken expiredToken = RefreshToken.builder()
                .userId(userId)
                .refreshToken("expired-token")
                .times(-1L)
                .build();
        Token issuedToken = Token.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .build();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(mock(User.class)));
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(expiredToken));
        when(jwtHandler.createTokens(claim)).thenReturn(issuedToken);

        Optional<Token> result = oAuthLoginPolicyService.issueTokenIfNoActiveSession(claim);

        assertThat(result).contains(issuedToken);
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(jwtHandler).createTokens(claim);
    }

    @Test
    void 사용자_락_대상_없으면_예외를_던진다() {
        Long userId = 1L;
        JwtUserClaim claim = new JwtUserClaim(userId, UserRole.USER, false);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthLoginPolicyService.issueTokenIfNoActiveSession(claim))
                .isInstanceOf(BusinessException.class);

        verify(refreshTokenRepository, never()).findByUserId(anyLong());
        verify(jwtHandler, never()).createTokens(any());
    }
}
