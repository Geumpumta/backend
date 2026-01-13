package com.gpt.geumpumtabackend.wifi.service;

import com.gpt.geumpumtabackend.wifi.config.CampusWiFiProperties;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CampusWiFiValidationService {

    private final CampusWiFiProperties wifiProperties;

    /**
     * 캠퍼스 WiFi 검증 (캐시 적용)
     *
     * @param gatewayIp 게이트웨이 IP
     * @param clientIp 클라이언트 IP
     * @return 검증 결과
     */
    @Cacheable(value = "wifiValidation", key = "#gatewayIp + ':' + #clientIp")
    public WiFiValidationResult validateCampusWiFi(String gatewayIp, String clientIp) {
        try {
            log.info("WiFi 검증 실행 (캐시 미스) - Gateway IP: {}, Client IP: {}", gatewayIp, clientIp);

            // 캠퍼스 내부인지 확인
            boolean isInCampus = isInCampusNetwork(gatewayIp, clientIp);

            if (isInCampus) {
                return WiFiValidationResult.valid("캠퍼스 네트워크입니다");
            } else {
                return WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다");
            }

        } catch (Exception e) {
            log.error("WiFi 검증 중 오류 발생", e);
            return WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캠퍼스 네트워크 검증 (실제 로직)
     */
    private boolean isInCampusNetwork(String gatewayIp, String ipAddress) {
        // 설정 파일 Wi-fi 목록 불러오기
        List<CampusWiFiProperties.WiFiNetwork> activeNetworks = wifiProperties.networks()
            .stream()
            .filter(CampusWiFiProperties.WiFiNetwork::active)
            .toList();

        for (CampusWiFiProperties.WiFiNetwork network : activeNetworks) {
            // 1. Gateway IP 체크 (SSID 대신 사용)
            if (!network.isValidGatewayIP(gatewayIp)) {
                continue;
            }
            // 2. Client IP가 해당 네트워크 범위 내인지 체크
            if (network.isValidIP(ipAddress)) {
                return true; // 매칭되면 즉시 성공!
            }
        }
        return false;
    }
}
