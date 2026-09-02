package com.transport.reporting.security;

import com.transport.reporting.config.GoogleOAuthConfiguredCondition;
import com.transport.reporting.config.GoogleOAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirige vers le frontend en cas d'échec ou d'annulation de la connexion Google.
 */
@Component
@Conditional(GoogleOAuthConfiguredCondition.class)
public class GoogleOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleOAuth2LoginFailureHandler(GoogleOAuthProperties googleOAuthProperties) {
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : "Connexion Google annulée ou refusée.";

        String redirectUrl = UriComponentsBuilder
                .fromUriString(googleOAuthProperties.getFrontendCallbackUrl())
                .queryParam("error", "google_auth_failed")
                .queryParam("message", URLEncoder.encode(message, StandardCharsets.UTF_8))
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
