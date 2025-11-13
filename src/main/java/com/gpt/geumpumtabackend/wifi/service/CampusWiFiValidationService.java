package com.gpt.geumpumtabackend.wifi.service;

import com.gpt.geumpumtabackend.global.wifi.IpUtil;
import com.gpt.geumpumtabackend.wifi.config.CampusWiFiProperties;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CampusWiFiValidationService {
    
    private final CampusWiFiProperties wifiProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis 캐시 키 접두사
    private static final String WIFI_CACHE_KEY_PREFIX = "campus_wifi_validation:";
    

    public WiFiValidationResult validateCampusWiFi(Integer gatewayIp, String bssid, HttpServletRequest request) {

        try {
            // 서버에서 클라이언트 IP 추출
            String ipAddress = IpUtil.getClientIp(request);
            log.info("Wi-Fi validation request - Gateway IP: {}, BSSID: {}, Client IP: {}", gatewayIp, bssid, ipAddress);

            // 캠퍼스 내부인지 확인
            boolean isInCampus = isInCampusNetwork(gatewayIp, bssid, ipAddress);

            if (isInCampus) {
                cacheValidationResult(gatewayIp, ipAddress, true);
                return WiFiValidationResult.valid("캠퍼스 네트워크입니다");
            } else {
                cacheValidationResult(gatewayIp, ipAddress, false);
                return WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다");
            }

        } catch (Exception e) {
            log.error("Wi-Fi validation error", e);
            return WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    

    public WiFiValidationResult validateFromCache(Integer gatewayIp, String bssid, HttpServletRequest request) {
        try {
            // 서버에서 클라이언트 IP 추출
            String ipAddress = IpUtil.getClientIp(request);
            // Gateway IP와 클라이언트 IP를 통해 키를 생성 후 Redis에서 조회
            String cacheKey = buildCacheKey(gatewayIp.toString(), ipAddress);
            Boolean cachedResult = (Boolean) redisTemplate.opsForValue().get(cacheKey);
                
            if (cachedResult != null) {
                return cachedResult
                        ? WiFiValidationResult.valid("캠퍼스 네트워크입니다 (캐시)")
                        : WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다 (캐시)");
            }
            
            // 캐시에 없으면 전체 검증 수행
            return validateCampusWiFi(gatewayIp, bssid, request);
            
        } catch (Exception e) {
            return WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    private boolean isInCampusNetwork(Integer gatewayIp, String bssid, String ipAddress) {

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
            if (bssid != null && !bssid.isEmpty() && !network.isValidBSSID(bssid)) {
                continue;
            }
            if (network.isValidIP(ipAddress)) {
                return true; // 매칭되면 즉시 성공!
            }
        }
        return false;
    }
    

    private String buildCacheKey(String gatewayIp, String ipAddress) {
        return WIFI_CACHE_KEY_PREFIX + gatewayIp + ":" + ipAddress;
    }
    

    private void cacheValidationResult(Integer gatewayIp, String ipAddress, boolean isValid) {
        String cacheKey = buildCacheKey(gatewayIp.toString(), ipAddress);
        Duration ttl = Duration.ofMinutes(wifiProperties.validation().cacheTtlMinutes());
        redisTemplate.opsForValue().set(cacheKey, isValid, ttl);
    }
}
