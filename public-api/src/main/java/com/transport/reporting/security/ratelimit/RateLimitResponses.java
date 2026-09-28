package com.transport.reporting.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.reporting.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class RateLimitResponses {

    static final String MESSAGE = "Trop de requêtes. Veuillez réessayer plus tard.";

    private RateLimitResponses() {
    }

    static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response, int retryAfterSeconds)
            throws IOException {
        int retry = Math.max(1, retryAfterSeconds);
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retry));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                429,
                "Too Many Requests",
                MESSAGE,
                "RATE_LIMIT",
                request.getRequestURI(),
                null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
