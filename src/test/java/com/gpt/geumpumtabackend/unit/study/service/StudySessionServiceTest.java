package com.gpt.geumpumtabackend.unit.study.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.study.service.StudySessionService;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("unit-test")  // 단위테스트 프로필 사용 (Redis 비활성화)
@DisplayName("StudySessionService 단위 테스트")
class StudySessionServiceTest {

    @Mock
    private StudySessionRepository studySessionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CampusWiFiValidationService wifiValidationService;
    
    @InjectMocks
    private StudySessionService studySessionService;

    @Nested
    @DisplayName("공부 세션 시작")
    class StartStudySession {

        @Test
        @DisplayName("Wi-Fi 검증 성공 시 세션이 정상 시작된다")
        void startStudySession_WiFi검증성공_세션시작성공() {
            // Given
            Long userId = 1L;
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            
            StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);
            
            User testUser = createTestUser(userId, "테스트사용자", Department.SOFTWARE);
            
            // Mock StudySession with proper ID
            StudySession mockSession = mock(StudySession.class);
            given(mockSession.getId()).willReturn(100L);
            
            // Mock 설정
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));
            given(userRepository.findById(userId))
                .willReturn(Optional.of(testUser));
            given(studySessionRepository.save(any(StudySession.class)))
                .willReturn(mockSession);

            // When
            StudyStartResponse response = studySessionService.startStudySession(request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.studySessionId()).isEqualTo(100L);
            
            verify(wifiValidationService).validateFromCache(gatewayIp, clientIp);
            verify(userRepository).findById(userId);
            verify(studySessionRepository).save(any(StudySession.class));
        }

        @Test
        @DisplayName("Wi-Fi 검증 실패(INVALID) 시 WIFI_NOT_CAMPUS_NETWORK 예외가 발생한다")
        void startStudySession_WiFi검증실패_INVALID_예외발생() {
            // Given
            Long userId = 1L;
            String gatewayIp = "192.168.10.1";  // 잘못된 게이트웨이
            String clientIp = "192.168.10.100";
            LocalDateTime startTime = LocalDateTime.now();
            
            StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);
            
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다"));

            // When & Then
            assertThatThrownBy(() -> studySessionService.startStudySession(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.WIFI_NOT_CAMPUS_NETWORK);
            
            verify(wifiValidationService).validateFromCache(gatewayIp, clientIp);
            verify(userRepository, never()).findById(anyLong());
            verify(studySessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wi-Fi 검증 에러(ERROR) 시 WIFI_VALIDATION_ERROR 예외가 발생한다")
        void startStudySession_WiFi검증에러_ERROR_예외발생() {
            // Given
            Long userId = 1L;
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            LocalDateTime startTime = LocalDateTime.now();
            
            StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);
            
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.error("Redis 연결 실패"));

            // When & Then
            assertThatThrownBy(() -> studySessionService.startStudySession(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.WIFI_VALIDATION_ERROR);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 세션 시작 시 USER_NOT_FOUND 예외가 발생한다")
        void startStudySession_존재하지않는사용자_예외발생() {
            // Given
            Long userId = 999L;
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            LocalDateTime startTime = LocalDateTime.now();
            
            StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);
            
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));
            given(userRepository.findById(userId))
                .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> studySessionService.startStudySession(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.USER_NOT_FOUND);
        }
    }

    // 하트비트 기능이 현재 서비스에서 제거된 상태로 확인됨
    // updateHeartBeat 메서드가 존재하지 않으므로 관련 테스트 제거

    @Nested
    @DisplayName("공부시간 계산 로직")
    class StudySessionCalculation {

        @Test
        @DisplayName("정상적인 공부세션 종료시 올바른 시간이 계산된다")
        void 정상적인_공부세션_종료시_올바른_시간이_계산된다() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 30);
            User testUser = createTestUser(1L, "테스트사용자", Department.SOFTWARE);
            
            StudySession session = new StudySession();
            
            // When
            session.startStudySession(startTime, testUser);
            session.endStudySession(endTime);
            
            // Then
            assertThat(session.getTotalMillis()).isEqualTo(5400000L); // 90분 = 90 * 60 * 1000ms
            assertThat(session.getStatus()).isEqualTo(com.gpt.geumpumtabackend.study.domain.StudyStatus.FINISHED);
            assertThat(session.getEndTime()).isEqualTo(endTime);
        }

        @Test
        @DisplayName("매우 짧은 세션(1초)도 올바르게 계산된다")
        void 매우_짧은_세션도_올바르게_계산된다() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 0, 1);
            User testUser = createTestUser(1L, "테스트사용자", Department.SOFTWARE);
            
            StudySession session = new StudySession();
            
            // When
            session.startStudySession(startTime, testUser);
            session.endStudySession(endTime);
            
            // Then
            assertThat(session.getTotalMillis()).isEqualTo(1000L); // 1초 = 1000ms
        }

        @Test
        @DisplayName("긴 세션(12시간)도 올바르게 계산된다")
        void 긴_세션도_올바르게_계산된다() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 21, 0);
            User testUser = createTestUser(1L, "테스트사용자", Department.SOFTWARE);
            
            StudySession session = new StudySession();
            
            // When
            session.startStudySession(startTime, testUser);
            session.endStudySession(endTime);
            
            // Then
            assertThat(session.getTotalMillis()).isEqualTo(43200000L); // 12시간 = 12 * 60 * 60 * 1000ms
        }

        @Test
        @DisplayName("자정을 넘어가는 세션도 올바르게 계산된다")
        void 자정을_넘어가는_세션도_올바르게_계산된다() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 23, 30);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 2, 1, 30);
            User testUser = createTestUser(1L, "테스트사용자", Department.SOFTWARE);
            
            StudySession session = new StudySession();
            
            // When
            session.startStudySession(startTime, testUser);
            session.endStudySession(endTime);
            
            // Then
            assertThat(session.getTotalMillis()).isEqualTo(7200000L); // 2시간 = 2 * 60 * 60 * 1000ms
        }

        @Test
        @DisplayName("초기 세션 생성시 상태가 올바르게 설정된다")
        void 초기_세션_생성시_상태가_올바르게_설정된다() {
            // Given
            LocalDateTime startTime = LocalDateTime.now();
            User testUser = createTestUser(1L, "테스트사용자", Department.SOFTWARE);
            StudySession session = new StudySession();
            
            // When
            session.startStudySession(startTime, testUser);
            
            // Then
            assertThat(session.getStartTime()).isEqualTo(startTime);
            assertThat(session.getUser()).isEqualTo(testUser);
            assertThat(session.getStatus()).isEqualTo(com.gpt.geumpumtabackend.study.domain.StudyStatus.STARTED);
            assertThat(session.getEndTime()).isNull();
            assertThat(session.getTotalMillis()).isNull();
        }
    }

    // 테스트 데이터 생성 헬퍼 메서드
    private User createTestUser(Long id, String name, Department department) {
        User user = User.builder()
            .name(name)
            .email("test@kumoh.ac.kr")
            .department(department)
            .picture("test.jpg")
            .role(com.gpt.geumpumtabackend.user.domain.UserRole.USER)
            .provider(com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider.GOOGLE)
            .providerId("test-provider-id")
            .build();
        
        // 테스트용 ID 설정 (Reflection 사용)
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test user ID", e);
        }
        
        return user;
    }

    private StudySession createTestStudySession(Long id, User user, LocalDateTime startTime) {
        StudySession session = new StudySession();
        session.startStudySession(startTime, user);
        // id는 실제로는 JPA가 설정하지만 테스트를 위해 reflection 사용하거나
        // 별도의 테스트용 생성자/메서드를 만들어 설정
        return session;
    }
}