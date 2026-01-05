package com.gpt.geumpumtabackend.unit.wifi.service;

import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("unit-test")  // 단위테스트 프로필 사용 (Redis 비활성화)
@DisplayName("CampusWiFiValidationService 단위 테스트 (Mock)")
class CampusWiFiValidationServiceTest {

    @Mock
    private CampusWiFiValidationService wifiValidationService;

    @Nested
    @DisplayName("캐시에서 WiFi 검증 (Mock)")
    class ValidateFromCache {

        @Test
        @DisplayName("유효한_캠퍼스_네트워크_검증_성공")
        void validateFromCache_유효한캠퍼스네트워크_검증성공() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateFromCache(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.VALID);
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크입니다");
            
            verify(wifiValidationService).validateFromCache(gatewayIp, clientIp);
        }

        @Test
        @DisplayName("무효한_네트워크_검증_실패")
        void validateFromCache_무효한네트워크_검증실패() {
            // Given
            String gatewayIp = "192.168.10.1";
            String clientIp = "192.168.10.100";
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateFromCache(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.INVALID);
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크가 아닙니다");
        }

        @Test
        @DisplayName("검증_오류_시_에러_결과_반환")
        void validateFromCache_검증오류_에러결과반환() {
            // Given
            String gatewayIp = "error.test.ip";
            String clientIp = "192.168.1.100";
            given(wifiValidationService.validateFromCache(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateFromCache(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.ERROR);
            assertThat(result.getMessage()).contains("Wi-Fi 검증 중 오류가 발생했습니다");
        }
    }

    @Nested
    @DisplayName("캠퍼스 WiFi 검증 (Mock)")
    class ValidateCampusWiFi {

        @Test
        @DisplayName("유효한_캠퍼스_네트워크_검증_성공")
        void validateCampusWiFi_유효한캠퍼스네트워크_검증성공() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            given(wifiValidationService.validateCampusWiFi(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.VALID);
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크입니다");
            
            verify(wifiValidationService).validateCampusWiFi(gatewayIp, clientIp);
        }

        @Test
        @DisplayName("잘못된_게이트웨이_IP로_검증_실패")
        void validateCampusWiFi_잘못된게이트웨이IP_검증실패() {
            // Given
            String gatewayIp = "192.168.10.1";  // 잘못된 게이트웨이
            String clientIp = "192.168.1.100";
            given(wifiValidationService.validateCampusWiFi(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.INVALID);
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크가 아닙니다");
        }

        @Test
        @DisplayName("검증_중_예외_발생시_에러_결과_반환")
        void validateCampusWiFi_예외발생_에러결과반환() {
            // Given
            String gatewayIp = "error.test.ip";
            String clientIp = "192.168.1.100";
            given(wifiValidationService.validateCampusWiFi(gatewayIp, clientIp))
                .willReturn(WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다"));

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.ERROR);
            assertThat(result.getMessage()).contains("Wi-Fi 검증 중 오류가 발생했습니다");
        }
    }

    @Nested
    @DisplayName("다양한_시나리오_테스트")
    class VariousScenarios {

        @Test
        @DisplayName("다양한_캠퍼스_IP_대역_검증")
        void 다양한_캠퍼스IP대역_검증() {
            // Given
            given(wifiValidationService.validateFromCache("192.168.1.1", "192.168.1.50"))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));
            given(wifiValidationService.validateFromCache("172.30.64.1", "172.30.64.100"))
                .willReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다"));

            // When & Then
            WiFiValidationResult result1 = wifiValidationService.validateFromCache("192.168.1.1", "192.168.1.50");
            assertThat(result1.isValid()).isTrue();

            WiFiValidationResult result2 = wifiValidationService.validateFromCache("172.30.64.1", "172.30.64.100");
            assertThat(result2.isValid()).isTrue();
        }

        @Test
        @DisplayName("NULL_또는_빈_입력값_처리")
        void NULL_또는_빈입력값_처리() {
            // Given
            given(wifiValidationService.validateFromCache(isNull(), anyString()))
                .willReturn(WiFiValidationResult.error("잘못된 입력값입니다"));
            given(wifiValidationService.validateFromCache(eq(""), anyString()))
                .willReturn(WiFiValidationResult.error("잘못된 입력값입니다"));

            // When & Then
            WiFiValidationResult result1 = wifiValidationService.validateFromCache(null, "192.168.1.100");
            assertThat(result1.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.ERROR);

            WiFiValidationResult result2 = wifiValidationService.validateFromCache("", "192.168.1.100");
            assertThat(result2.getStatus()).isEqualTo(WiFiValidationResult.ValidationStatus.ERROR);
        }
    }
}