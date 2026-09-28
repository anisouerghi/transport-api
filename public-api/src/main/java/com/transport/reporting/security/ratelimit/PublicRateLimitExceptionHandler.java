package com.transport.reporting.security.ratelimit;

import com.transport.reporting.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Réponse 429 si l'exception atteint le dispatcher MVC.
 * Le filtre de sécurité écrit la même réponse lorsqu'il bloque avant le contrôleur.
 */
@RestControllerAdvice
public class PublicRateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handle(RateLimitExceededException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                429,
                "Too Many Requests",
                RateLimitResponses.MESSAGE,
                "RATE_LIMIT",
                request.getRequestURI(),
                null);
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }
}
