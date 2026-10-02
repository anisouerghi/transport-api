package com.transport.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

/**
 * Enregistrement OAuth2 Google pour public-api (voyageur uniquement).
 */
@Configuration
@Conditional(GoogleOAuthConfiguredCondition.class)
public class GoogleOAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2ClientConfig.class);
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(GoogleOAuthProperties properties) {
        String secret = properties.getClientSecret();
        boolean secretConfigured = StringUtils.hasText(secret);
        boolean officialSecretPrefix = secretConfigured && secret.startsWith("GOCSPX-");
        log.info(
                "Google OAuth client ID: configured={}, redirect URI: {}, secret: configured={}, officialPrefix={}, token URI: {}, authMethod: client_secret_post",
                StringUtils.hasText(properties.getClientId()),
                properties.getRedirectUri(),
                secretConfigured,
                officialSecretPrefix,
                TOKEN_URI);
        if (secretConfigured && !officialSecretPrefix) {
            log.error(
                    "Google OAuth secret rejected: it does not start with GOCSPX-. "
                            + "Google returns 401 invalid_token_response when this secret does not belong to the same OAuth client. "
                            + "Set GOOGLE_CLIENT_SECRET to the Web client secret of the configured client id, then restart public-api.");
        }

        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(properties.getClientId())
                .clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.getRedirectUri())
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri(TOKEN_URI)
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .issuerUri("https://accounts.google.com")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
