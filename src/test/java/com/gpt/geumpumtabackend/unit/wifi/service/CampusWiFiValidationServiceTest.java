package com.gpt.geumpumtabackend.unit.wifi.service;

import com.gpt.geumpumtabackend.wifi.config.CampusWiFiProperties;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("unit-test")
@DisplayName("CampusWiFiValidationService 단위 테스트")
class CampusWiFiValidationServiceTest {

    @Mock
    private CampusWiFiProperties wifiProperties;

    @InjectMocks
    private CampusWiFiValidationService wifiValidationService;

    @Nested
    @DisplayName("캠퍼스 WiFi 검증 로직")
    class ValidateCampusWiFi {

        @Test
        @DisplayName("유효한_네트워크_정보가_매칭되면_검증_성공")
        void 매칭성공() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";

            CampusWiFiProperties.WiFiNetwork network = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(network));

            given(network.active()).willReturn(true);
            given(network.isValidGatewayIP(gatewayIp)).willReturn(true);
            given(network.isValidIP(clientIp)).willReturn(true);

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크입니다");
        }

        @Test
        @DisplayName("매칭되는_네트워크가_없으면_검증_실패")
        void 매칭실패() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";

            CampusWiFiProperties.WiFiNetwork network = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(network));

            given(network.active()).willReturn(true);
            given(network.isValidGatewayIP(gatewayIp)).willReturn(false); // 게이트웨이 불일치

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크가 아닙니다");
        }

        @Test
        @DisplayName("게이트웨이_IP는_매칭되지만_클라이언트_IP_범위가_다르면_검증_실패")
        void 게이트웨이매칭_클라이언트불일치() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.2.100"; // 다른 대역

            CampusWiFiProperties.WiFiNetwork network = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(network));

            given(network.active()).willReturn(true);
            given(network.isValidGatewayIP(gatewayIp)).willReturn(true);
            given(network.isValidIP(clientIp)).willReturn(false); // IP 범위 불일치

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크가 아닙니다");
        }

        @Test
        @DisplayName("활성화되지_않은_네트워크는_검증에서_제외된다")
        void 비활성화네트워크제외() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";

            CampusWiFiProperties.WiFiNetwork inactiveNetwork = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(inactiveNetwork));

            given(inactiveNetwork.active()).willReturn(false); // 비활성화

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isFalse();
            verify(inactiveNetwork, never()).isValidGatewayIP(any()); // 비활성화 네트워크는 체크 안함
        }

        @Test
        @DisplayName("여러_네트워크_중_하나라도_매칭되면_검증_성공")
        void 여러네트워크중_하나매칭() {
            // Given
            String gatewayIp = "172.30.64.1";
            String clientIp = "172.30.64.100";

            CampusWiFiProperties.WiFiNetwork network1 = mock(CampusWiFiProperties.WiFiNetwork.class);
            CampusWiFiProperties.WiFiNetwork network2 = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(network1, network2));

            // network1은 불일치
            given(network1.active()).willReturn(true);
            given(network1.isValidGatewayIP(gatewayIp)).willReturn(false);

            // network2는 매칭
            given(network2.active()).willReturn(true);
            given(network2.isValidGatewayIP(gatewayIp)).willReturn(true);
            given(network2.isValidIP(clientIp)).willReturn(true);

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크입니다");
        }
    }
}
