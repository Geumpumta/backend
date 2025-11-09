package com.gpt.geumpumtabackend.wifi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "campus.wifi")
@Data
@NoArgsConstructor
public class CampusWiFiProperties {
    
    private List<WiFiNetwork> networks = new ArrayList<>();
    private ValidationConfig validation = new ValidationConfig();
    

    @Data
    @NoArgsConstructor
    public static class WiFiNetwork {
        private String name;                    // "중앙도서관"
        private String ssid;                    // "KIT-WiFi"
        private List<String> bssids = new ArrayList<>();  // MAC 주소들
        private List<String> ipRanges = new ArrayList<>(); // IP 대역들
        private Boolean active = true;          // 활성화 상태
        private String description;             // 설명
        


        public boolean isValidSSID(String ssid) {
            return this.ssid != null && this.ssid.equals(ssid);
        }
        

        public boolean isValidBSSID(String bssid) {
            if (bssids.isEmpty()) {
                return true; // BSSID 제한 없음
            }
            return bssids.contains(bssid);
        }
        

        public boolean isValidIP(String ipAddress) {
            return ipRanges.stream()
                .anyMatch(range -> isIpInRange(ipAddress, range));
        }
        

        private boolean isIpInRange(String ipAddress, String cidr) {
            try {
                SubnetUtils subnet = new SubnetUtils(cidr);
                SubnetUtils.SubnetInfo subnetInfo = subnet.getInfo();
                return subnetInfo.isInRange(ipAddress);
            } catch (Exception e) {
                return false;
            }
        }
    }
    

    @Data
    @NoArgsConstructor
    public static class ValidationConfig {
        private Boolean allowUnknownBssid = true;    // 미등록 BSSID 허용
        private Integer cacheTtlMinutes = 60;        // 캐시 유지 시간 (1시간)
        private Integer maxSsidLength = 32;          // SSID 최대 길이
    }
}