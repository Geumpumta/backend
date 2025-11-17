package com.gpt.geumpumtabackend.global.wifi;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");

        if(isUnknown(ip)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if(!isUnknown(xForwardedFor)) {
                ip = xForwardedFor.split(",")[0].trim();
            }
        }

        if(isUnknown(ip)){
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    private static boolean isUnknown(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
