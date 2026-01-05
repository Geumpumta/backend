package com.gpt.geumpumtabackend.integration.study.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.service.StudySessionService;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StudySessionService 통합 테스트")
class StudySessionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StudySessionService studySessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampusWiFiValidationService wifiValidationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = createAndSaveUser("통합테스트사용자", "integration@kumoh.ac.kr", Department.SOFTWARE);
    }

    @Test
    @DisplayName("유효한_캠퍼스_네트워크에서_학습_세션이_시작된다")
    void 유효한_캠퍼스_네트워크에서_학습_세션이_시작된다() {
        // Given - application-test.yml에 설정된 유효한 캠퍼스 IP 사용
        // gateway: 172.30.64.1, ip-range: 172.30.64.0/18
        String gatewayIp = "172.30.64.1";
        String clientIp = "172.30.64.100";
        StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);

        // When - 학습 세션 시작 (실제 WiFi 검증 통과)
        StudyStartResponse response1 = studySessionService.startStudySession(request, testUser.getId());

        // Then - 학습 세션이 성공적으로 시작됨
        assertThat(response1).isNotNull();
        assertThat(response1.studySessionId()).isNotNull();

        // When - 두 번째 호출도 정상 동작
        StudyStartResponse response2 = studySessionService.startStudySession(request, testUser.getId());

        // Then - 새로운 학습 세션이 생성됨
        assertThat(response2).isNotNull();
        assertThat(response2.studySessionId()).isNotNull();
        assertThat(response2.studySessionId()).isNotEqualTo(response1.studySessionId());
    }

    @Test
    @DisplayName("캠퍼스가_아닌_네트워크에서는_학습_세션_시작이_실패한다")
    void 캠퍼스가_아닌_네트워크에서는_학습_세션_시작이_실패한다() {
        // Given - 유효하지 않은 네트워크 IP
        String gatewayIp = "192.168.10.1";
        String clientIp = "192.168.10.100";
        StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);

        // When & Then - WiFi 검증 실패로 예외 발생
        assertThatThrownBy(() -> studySessionService.startStudySession(request, testUser.getId()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("유효한_게이트웨이이지만_클라이언트_IP가_범위를_벗어나면_실패한다")
    void 유효한_게이트웨이이지만_클라이언트_IP가_범위를_벗어나면_실패한다() {
        // Given - 유효한 게이트웨이, 하지만 IP 범위 밖의 클라이언트 IP
        String gatewayIp = "172.30.64.1";
        String clientIp = "192.168.1.100";  // 172.30.64.0/18 범위 밖
        StudyStartRequest request = new StudyStartRequest(gatewayIp, clientIp);

        // When & Then - 클라이언트 IP 검증 실패
        assertThatThrownBy(() -> studySessionService.startStudySession(request, testUser.getId()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("WiFi_검증_결과가_Redis에_캐시된다")
    void WiFi_검증_결과가_Redis에_캐시된다() {
        // Given
        String gatewayIp = "172.30.64.1";
        String clientIp = "172.30.64.100";

        // When - 첫 번째 검증 (캐시 미스, 실제 검증 수행)
        var result1 = wifiValidationService.validateFromCache(gatewayIp, clientIp);

        // Then - 유효한 결과 반환
        assertThat(result1.isValid()).isTrue();
        assertThat(result1.getMessage()).isEqualTo("캠퍼스 네트워크입니다");

        // When - 두 번째 검증 (캐시 히트)
        var result2 = wifiValidationService.validateFromCache(gatewayIp, clientIp);

        // Then - 캐시된 결과 반환 (캐시 문구 포함)
        assertThat(result2.isValid()).isTrue();
        assertThat(result2.getMessage()).isEqualTo("캠퍼스 네트워크입니다 (캐시)");
    }

    @Test
    @DisplayName("IP_범위의_경계값도_정확히_검증된다")
    void IP_범위의_경계값도_정확히_검증된다() {
        // Given - 172.30.64.0/18 범위의 경계값 테스트
        String gatewayIp = "172.30.64.1";

        // 범위 내 첫 번째 IP
        String validIp1 = "172.30.64.1";
        // 범위 내 마지막 IP (172.30.127.254)
        String validIp2 = "172.30.127.254";

        // When & Then - 범위 내 IP는 통과
        var result1 = wifiValidationService.validateFromCache(gatewayIp, validIp1);
        assertThat(result1.isValid()).isTrue();

        var result2 = wifiValidationService.validateFromCache(gatewayIp, validIp2);
        assertThat(result2.isValid()).isTrue();

        // Given - 범위 밖 IP
        String invalidIp = "172.30.128.1";  // /18 범위 초과

        // When & Then - 범위 밖 IP는 실패
        var result3 = wifiValidationService.validateFromCache(gatewayIp, invalidIp);
        assertThat(result3.isValid()).isFalse();
    }

    // 헬퍼 메서드
    private User createAndSaveUser(String name, String email, Department department) {
        User user = User.builder()
            .name(name)
            .email(email)
            .department(department)
            .picture("profile.jpg")
            .role(UserRole.USER)
            .provider(com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider.GOOGLE)
            .providerId("test-provider-id-" + email)
            .build();
        return userRepository.save(user);
    }
}