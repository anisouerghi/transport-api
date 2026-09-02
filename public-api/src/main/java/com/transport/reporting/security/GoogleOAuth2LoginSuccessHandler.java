package com.transport.reporting.security;

import com.transport.reporting.config.GoogleOAuthConfiguredCondition;
import com.transport.reporting.config.GoogleOAuthProperties;
import com.transport.reporting.dto.PassengerAuthResponse;
import com.transport.reporting.service.PassengerAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Après validation Google par Spring Security OAuth2/OIDC, crée ou retrouve le voyageur
 * et redirige vers le frontend avec un code d'échange éphémère.
 */
@Component
@Conditional(GoogleOAuthConfiguredCondition.class)
public class GoogleOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String RETURN_URL_COOKIE = "oauth_return_url";

    private final PassengerAuthService passengerAuthService;
    private final GoogleOAuthCallbackCodeStore callbackCodeStore;
    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleOAuth2LoginSuccessHandler(
            PassengerAuthService passengerAuthService,
            GoogleOAuthCallbackCodeStore callbackCodeStore,
            GoogleOAuthProperties googleOAuthProperties) {
        this.passengerAuthService = passengerAuthService;
        this.callbackCodeStore = callbackCodeStore;
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            redirectWithError(response, "google_auth_failed", "Identité Google invalide.");
            return;
        }

        try {
            PassengerAuthResponse authResponse = passengerAuthService.authenticateGoogleUser(
                    oidcUser.getSubject(),
                    oidcUser.getEmail(),
                    Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                    oidcUser.getFullName());

            String exchangeCode = callbackCodeStore.issue(authResponse);
            String returnUrl = readReturnUrl(request);
            clearReturnUrlCookie(response);

            String redirectUrl = UriComponentsBuilder
                    .fromUriString(googleOAuthProperties.getFrontendCallbackUrl())
                    .queryParam("code", exchangeCode)
                    .queryParam("returnUrl", returnUrl)
                    .build()
                    .encode()
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (RuntimeException ex) {
            redirectWithError(response, "google_auth_failed", ex.getMessage());
        }
    }

    private void redirectWithError(HttpServletResponse response, String error, String message) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(googleOAuthProperties.getFrontendCallbackUrl())
                .queryParam("error", error)
                .queryParam("message", URLEncoder.encode(message, StandardCharsets.UTF_8))
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String readReturnUrl(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (RETURN_URL_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return sanitizeReturnUrl(cookie.getValue());
                }
            }
        }
        return "/accueil";
    }

    public static String sanitizeReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            return "/accueil";
        }
        String trimmed = returnUrl.trim();
        if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
            return trimmed;
        }
        return "/accueil";
    }

    private void clearReturnUrlCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(RETURN_URL_COOKIE, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
