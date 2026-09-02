package com.transport.reporting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.reporting.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Réponses JSON uniformes pour 401 / 403 (public-api).
 * Les navigations navigateur vers les endpoints OAuth ne reçoivent pas de JSON bloquant.
 */
@Component("publicSecurityExceptionHandler")
public class PublicSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public PublicSecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (isOAuthPath(request)) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Connexion Google indisponible.");
            return;
        }
        write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (isOAuthPath(request)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Connexion Google refusée.");
            return;
        }
        write(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private static boolean isOAuthPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/public/auth/google");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, null, null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
