package com.gpt.geumpumtabackend.unit.token.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.repository.UserSessionRepository;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.unit.config.BaseUnitTest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserSessionService 단위 테스트")
class UserSessionServiceTest extends BaseUnitTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private UserSessionService userSessionService;

    @Test
    @DisplayName("새 세션 생성 시 기존 ACTIVE 세션을 revoke하고 새 ACTIVE 세션을 저장한다")
    void 새_세션_생성시_기존_ACTIVE_세션을_revoke하고_새_세션을_저장한다() {
        Long userId = 1L;
        long refreshTokenExpireIn = 3600L;
        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);

        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSession userSession = userSessionService.createNewSession(userId, refreshTokenExpireIn);

        InOrder inOrder = inOrder(userSessionRepository);
        inOrder.verify(userSessionRepository).revokeActiveSessionsByUserId(userId);
        inOrder.verify(userSessionRepository).save(sessionCaptor.capture());
        assertThat(userSession).isSameAs(sessionCaptor.getValue());
        assertThat(userSession.getUserId()).isEqualTo(userId);
        assertThat(userSession.getSessionId()).isNotBlank();
        assertThat(userSession.getRefreshToken()).isNotBlank();
        assertThat(userSession.isActive()).isTrue();
        assertThat(userSession.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Refresh Token과 세션이 유효하면 세션을 반환한다")
    void RefreshToken과_세션이_유효하면_세션을_반환한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        String refreshToken = "refresh-token";
        UserSession userSession = createUserSession(userId, sessionId, refreshToken, LocalDateTime.now().plusHours(1));

        when(userSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(userSession));
        when(userSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(userSession));

        UserSession result = userSessionService.validateRefreshToken(userId, sessionId, refreshToken);

        assertThat(result).isEqualTo(userSession);
        verify(userSessionRepository).findByRefreshToken(refreshToken);
        verify(userSessionRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Refresh Token이 비어 있으면 REFRESH_TOKEN_NOT_EXIST 예외가 발생한다")
    void RefreshToken이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> userSessionService.validateRefreshToken(1L, "session-id", " "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.REFRESH_TOKEN_NOT_EXIST);
        verify(userSessionRepository, never()).findByRefreshToken(any());
    }

    @Test
    @DisplayName("Refresh Token이 존재하지 않으면 REFRESH_TOKEN_NOT_EXIST 예외가 발생한다")
    void RefreshToken이_존재하지_않으면_예외가_발생한다() {
        String refreshToken = "missing-refresh-token";

        when(userSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSessionService.validateRefreshToken(1L, "session-id", refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.REFRESH_TOKEN_NOT_EXIST);
    }

    @Test
    @DisplayName("Access Token의 sessionId와 Refresh Token의 세션이 다르면 TOKEN_NOT_MATCHED 예외가 발생한다")
    void sessionId가_일치하지_않으면_예외가_발생한다() {
        Long userId = 1L;
        String refreshToken = "refresh-token";
        UserSession userSession = createUserSession(userId, "saved-session-id", refreshToken, LocalDateTime.now().plusHours(1));

        when(userSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(userSession));

        assertThatThrownBy(() -> userSessionService.validateRefreshToken(userId, "request-session-id", refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.TOKEN_NOT_MATCHED);
        verify(userSessionRepository, never()).findBySessionId(any());
    }

    @Test
    @DisplayName("REVOKED 세션의 Refresh Token이면 SESSION_INVALID 예외가 발생한다")
    void REVOKED_세션의_RefreshToken이면_예외가_발생한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        String refreshToken = "refresh-token";
        UserSession userSession = createUserSession(userId, sessionId, refreshToken, LocalDateTime.now().plusHours(1));
        userSession.revoke();

        when(userSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(userSession));
        when(userSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(userSession));

        assertThatThrownBy(() -> userSessionService.validateRefreshToken(userId, sessionId, refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.SESSION_INVALID);
    }

    @Test
    @DisplayName("만료된 세션의 Refresh Token이면 SESSION_INVALID 예외가 발생한다")
    void 만료된_세션의_RefreshToken이면_예외가_발생한다() {
        Long userId = 1L;
        String sessionId = "session-id";
        String refreshToken = "refresh-token";
        UserSession userSession = createUserSession(userId, sessionId, refreshToken, LocalDateTime.now().minusSeconds(1));

        when(userSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(userSession));
        when(userSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(userSession));

        assertThatThrownBy(() -> userSessionService.validateRefreshToken(userId, sessionId, refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.SESSION_INVALID);
    }

    private UserSession createUserSession(Long userId, String sessionId, String refreshToken, LocalDateTime expiresAt) {
        return UserSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
    }
}
