package com.transport.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Enregistrement OAuth2 Google pour public-api (voyageur uniquement).
 */
@Configuration
@Conditional(GoogleOAuthConfiguredCondition.class)
public class GoogleOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(GoogleOAuthProperties properties) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.getRedirectUri())
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .issuerUri("https://accounts.google.com")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
