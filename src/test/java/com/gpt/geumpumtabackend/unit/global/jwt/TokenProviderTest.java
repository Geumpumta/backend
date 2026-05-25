package com.gpt.geumpumtabackend.unit.global.jwt;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtAuthentication;
import com.gpt.geumpumtabackend.global.jwt.JwtAuthenticationToken;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.jwt.TokenProvider;
import com.gpt.geumpumtabackend.global.jwt.exception.JwtAccessDeniedException;
import com.gpt.geumpumtabackend.global.jwt.exception.JwtAuthenticationException;
import com.gpt.geumpumtabackend.global.jwt.exception.JwtTokenInvalidException;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.service.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenProvider 단위 테스트")
class TokenProviderTest {

    @Mock
    private JwtHandler jwtHandler;

    @Mock
    private UserService userService;

    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("JWT와 ACTIVE 세션이 유효하면 JwtAuthentication을 반환한다")
    void JWT와_ACTIVE_세션이_유효하면_인증을_반환한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.USER, false);
        JwtAuthenticationToken token = new JwtAuthenticationToken("access-token");

        when(jwtHandler.parseToken(token.token())).thenReturn(claim);
        when(userSessionService.validateActiveSession(userId, sessionId))
                .thenReturn(createUserSession(userId, sessionId));

        Authentication authentication = tokenProvider.authenticate(token);

        assertThat(authentication).isInstanceOf(JwtAuthentication.class);
        JwtAuthentication jwtAuthentication = (JwtAuthentication) authentication;
        assertThat(jwtAuthentication.userId()).isEqualTo(userId);
        assertThat(jwtAuthentication.sessionId()).isEqualTo(sessionId);
        assertThat(jwtAuthentication.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("세션이 REVOKED 상태이면 SESSION_INVALID 인증 예외로 변환한다")
    void 세션이_REVOKED이면_SESSION_INVALID_인증_예외로_변환한다() {
        Long userId = 1L;
        String sessionId = "revoked-session-id";
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.USER, false);
        JwtAuthenticationToken token = new JwtAuthenticationToken("access-token");

        when(jwtHandler.parseToken(token.token())).thenReturn(claim);
        when(userSessionService.validateActiveSession(userId, sessionId))
                .thenThrow(new BusinessException(ExceptionType.SESSION_INVALID));

        assertThatThrownBy(() -> tokenProvider.authenticate(token))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExceptionType.SESSION_INVALID);
    }

    @Test
    @DisplayName("SESSION_INVALID가 아닌 BusinessException은 JWT_INVALID 예외로 변환한다")
    void 일반_BusinessException은_JWT_INVALID_예외로_변환한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.USER, false);
        JwtAuthenticationToken token = new JwtAuthenticationToken("access-token");

        when(jwtHandler.parseToken(token.token())).thenReturn(claim);
        when(userSessionService.validateActiveSession(userId, sessionId))
                .thenThrow(new BusinessException(ExceptionType.USER_NOT_FOUND));

        assertThatThrownBy(() -> tokenProvider.authenticate(token))
                .isInstanceOf(JwtTokenInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExceptionType.JWT_INVALID);
    }

    @Test
    @DisplayName("예상하지 못한 예외는 JWT_INVALID 예외로 변환한다")
    void 예상하지_못한_예외는_JWT_INVALID_예외로_변환한다() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("access-token");

        when(jwtHandler.parseToken(token.token())).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> tokenProvider.authenticate(token))
                .isInstanceOf(JwtTokenInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExceptionType.JWT_INVALID);
    }

    @Test
    @DisplayName("ADMIN 토큰이지만 DB 권한이 ADMIN이 아니면 접근 거부 예외가 발생한다")
    void ADMIN_토큰과_DB권한이_다르면_접근_거부_예외가_발생한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        JwtUserClaim claim = JwtUserClaim.create(userId, sessionId, UserRole.ADMIN, false);
        JwtAuthenticationToken token = new JwtAuthenticationToken("access-token");

        when(jwtHandler.parseToken(token.token())).thenReturn(claim);
        when(userService.isAdmin(userId)).thenReturn(false);

        assertThatThrownBy(() -> tokenProvider.authenticate(token))
                .isInstanceOf(JwtAccessDeniedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExceptionType.ACCESS_DENIED);
        verify(userSessionService, never()).validateActiveSession(userId, sessionId);
    }

    private UserSession createUserSession(Long userId, String sessionId) {
        return UserSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
