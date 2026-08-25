package com.transport.reporting.common.util;

public class UserAgentParser {
    
    public static String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("opera") || ua.contains("opr")) return "Opera";
        if (ua.contains("msie") || ua.contains("trident")) return "Internet Explorer";
        return "Other";
    }
    
    public static String detectOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("linux") && !ua.contains("android")) return "Linux";
        if (ua.contains("android")) return "Android";
        if (ua.contains("ios") || ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        return "Other";
    }
}