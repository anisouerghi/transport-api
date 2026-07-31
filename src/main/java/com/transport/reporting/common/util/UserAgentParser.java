package com.transport.reporting.common.util;

/**
 * Extraction simplifiée du navigateur et du système d'exploitation
 * à partir d'un User-Agent HTTP (sans dépendance tierce).
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Inconnu";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Microsoft Edge";
        }
        if (ua.contains("chrome/") && !ua.contains("edg/")) {
            return "Google Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Mozilla Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("msie") || ua.contains("trident/")) {
            return "Internet Explorer";
        }
        return "Autre";
    }

    public static String detectOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Inconnu";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows nt")) {
            return "Windows";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Autre";
    }
}
