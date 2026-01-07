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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CampusWiFiValidationService wifiValidationService;

    @Nested
    @DisplayName("캐시에서 WiFi 검증")
    class ValidateFromCache {

        @Test
        @DisplayName("캐시에_값이_있으면_캐시_결과를_반환한다_VALID")
        void 캐시있음_성공() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            String cacheKey = "campus_wifi_validation:" + gatewayIp + ":" + clientIp;
            
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn("true");

            // When
            WiFiValidationResult result = wifiValidationService.validateFromCache(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).contains("(캐시)");
            verify(valueOperations).get(cacheKey);
            verify(wifiProperties, never()).networks();
        }

        @Test
        @DisplayName("캐시에_값이_없으면_실제_검증을_수행한다")
        void 캐시없음_실제검증수행() {
            // Given
            String gatewayIp = "192.168.1.1";
            String clientIp = "192.168.1.100";
            String cacheKey = "campus_wifi_validation:" + gatewayIp + ":" + clientIp;

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            
            // 실제 검증 로직을 위한 설정
            CampusWiFiProperties.WiFiNetwork network = mock(CampusWiFiProperties.WiFiNetwork.class);
            given(wifiProperties.networks()).willReturn(List.of(network));
            given(wifiProperties.validation()).willReturn(new CampusWiFiProperties.ValidationConfig(5));
            given(network.active()).willReturn(true);
            given(network.isValidGatewayIP(gatewayIp)).willReturn(true);
            given(network.isValidIP(clientIp)).willReturn(true);

            // When
            WiFiValidationResult result = wifiValidationService.validateFromCache(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isTrue();
            verify(valueOperations).set(eq(cacheKey), eq("true"), any());
        }
    }

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
            given(wifiProperties.validation()).willReturn(new CampusWiFiProperties.ValidationConfig(5));
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

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
            given(wifiProperties.validation()).willReturn(new CampusWiFiProperties.ValidationConfig(5));
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            given(network.active()).willReturn(true);
            given(network.isValidGatewayIP(gatewayIp)).willReturn(false); // 게이트웨이 불일치

            // When
            WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("캠퍼스 네트워크가 아닙니다");
        }
    }
}
