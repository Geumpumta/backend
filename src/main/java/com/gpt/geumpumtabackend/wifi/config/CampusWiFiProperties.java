package com.gpt.geumpumtabackend.wifi.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;


@ConfigurationProperties(prefix = "campus.wifi")
@Slf4j
public record CampusWiFiProperties(
        List<WiFiNetwork> networks,
        ValidationConfig validation) {
        
    public CampusWiFiProperties {
        // 기본값 설정
        if (networks == null) {
            networks = List.of();
        }
        if (validation == null) {
            validation = new ValidationConfig(60, 32);
        }
    }

    public record WiFiNetwork(String name,
                               String ssid, 
                               List<String> bssids,
                               List<String> ipRanges,
                               Boolean active,
                               String description) {

        public boolean isValidSSID(String ssid) {
            return this.ssid != null && this.ssid.equals(ssid);
        }

        public boolean isValidBSSID(String bssid) {
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
                log.warn("Invalid CIDR format or IP address: cidr={}, ip={}", cidr, ipAddress, e);
                return false;
            }
        }
    }
    



    public record ValidationConfig(Integer cacheTtlMinutes,
                                   Integer maxSsidLength) {

    }
}