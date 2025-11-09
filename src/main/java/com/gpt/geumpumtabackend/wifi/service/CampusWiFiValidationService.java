package com.gpt.geumpumtabackend.wifi.service;

import com.gpt.geumpumtabackend.wifi.config.CampusWiFiProperties;
import com.gpt.geumpumtabackend.wifi.dto.WiFiValidationResult;
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
    

    public WiFiValidationResult validateCampusWiFi(String ssid, String bssid, String ipAddress) {

        try {
            // 캠퍼스 내부인지 확인
            boolean isInCampus = isInCampusNetwork(ssid, bssid, ipAddress);

            if (isInCampus) {
                cacheValidationResult(ssid, ipAddress, true);
                return WiFiValidationResult.valid("캠퍼스 네트워크입니다");
            } else {
                cacheValidationResult(ssid, ipAddress, false);
                return WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다");
            }

        } catch (Exception e) {
            return WiFiValidationResult.error("Wi-Fi 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    

    public WiFiValidationResult validateFromCache(String ssid, String bssid, String ipAddress) {

        // IP 주소와 SSID를 통해 키를 생성 후  Redis에서 조회
        String cacheKey = buildCacheKey(ssid, ipAddress);
        Boolean cachedResult = (Boolean) redisTemplate.opsForValue().get(cacheKey);
            
        if (cachedResult != null) {
            return cachedResult
                    ? WiFiValidationResult.valid("캠퍼스 네트워크입니다 (캐시)")
                    : WiFiValidationResult.invalid("캠퍼스 네트워크가 아닙니다 (캐시)");
            }
        // 캐시에 없으면 전체 검증 수행
        return validateCampusWiFi(ssid, bssid, ipAddress);
    }


    private boolean isInCampusNetwork(String ssid, String bssid, String ipAddress) {
        List<CampusWiFiProperties.WiFiNetwork> activeNetworks = wifiProperties.networks()
            .stream()
            .filter(CampusWiFiProperties.WiFiNetwork::active)
            .toList();

        for (CampusWiFiProperties.WiFiNetwork network : activeNetworks) {
            // 1. SSID 체크
            if (!network.isValidSSID(ssid)) {
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
    

    private String buildCacheKey(String ssid, String ipAddress) {
        return WIFI_CACHE_KEY_PREFIX + ssid + ":" + ipAddress;
    }
    

    private void cacheValidationResult(String ssid, String ipAddress, boolean isValid) {
        String cacheKey = buildCacheKey(ssid, ipAddress);
        Duration ttl = Duration.ofMinutes(wifiProperties.validation().cacheTtlMinutes());
        redisTemplate.opsForValue().set(cacheKey, isValid, ttl);
    }
}
