package com.transport.reporting.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Accès au contexte HTTP courant (IP, User-Agent) depuis les services métier.
 */
public final class RequestMetadata {

    private RequestMetadata() {
    }

    public static String currentIpAddress() {
        try {
            HttpServletRequest request = currentRequest();
            if (request == null) {
                return "0.0.0.0"; // Valeur par défaut
            }
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String ip = request.getRemoteAddr();
            return ip != null ? ip : "0.0.0.0";
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public static String currentUserAgent() {
        try {
            HttpServletRequest request = currentRequest();
            if (request == null) {
                return "unknown"; // Valeur par défaut
            }
            String userAgent = request.getHeader("User-Agent");
            return userAgent != null ? userAgent : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static HttpServletRequest currentRequest() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                return servletAttrs.getRequest();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}