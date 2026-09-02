package com.transport.reporting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Google OAuth 2.0 / OIDC pour l'authentification voyageur.
 * <p>
 * Le {@code client_secret} est injecté uniquement via variable d'environnement ({@code GOOGLE_CLIENT_SECRET}),
 * jamais en dur dans le code ni dans Git.
 */
@Component
@ConfigurationProperties(prefix = "app.google")
public class GoogleOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private String frontendCallbackUrl = "";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendCallbackUrl() {
        return frontendCallbackUrl;
    }

    public void setFrontendCallbackUrl(String frontendCallbackUrl) {
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank()
                && frontendCallbackUrl != null && !frontendCallbackUrl.isBlank();
    }
}
