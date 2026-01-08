package com.gpt.geumpumtabackend.unit.config;

import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import com.gpt.geumpumtabackend.wifi.service.CampusWiFiValidationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestWiFiMockConfig {

    @Bean
    @Primary
    public CampusWiFiValidationService mockWiFiValidationService() {
        CampusWiFiValidationService mock = mock(CampusWiFiValidationService.class);
        
        // 기본적으로 캠퍼스 네트워크로 인식하도록 설정 (192.168.1.x)
        when(mock.validateFromCache("192.168.1.1", anyString()))
            .thenReturn(WiFiValidationResult.valid("캠퍼스 네트워크입니다 (Mock)"));
            
        // 캠퍼스가 아닌 네트워크 (192.168.10.x)
        when(mock.validateFromCache("192.168.10.1", anyString()))
            .thenReturn(WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다 (Mock)"));
            
        // 에러 시뮬레이션용 (특정 IP에서 에러 발생)
        when(mock.validateFromCache("error.test.ip", anyString()))
            .thenReturn(WiFiValidationResult.error("Redis 연결 실패 (Mock)"));
            
        return mock;
    }
}