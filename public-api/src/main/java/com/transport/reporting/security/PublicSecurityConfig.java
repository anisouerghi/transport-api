package com.transport.reporting.security;

import com.transport.reporting.config.GoogleOAuthProperties;
import com.transport.reporting.security.ratelimit.PublicRateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Sécurité Public API : routes voyageur uniquement (pas d'admin).
 * OAuth Google et JWT coexistent sur une seule chaîne.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class PublicSecurityConfig {

    private static final String[] GOOGLE_OAUTH_PATHS = {
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/public/auth/google",
            "/api/public/auth/google/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PublicRateLimitFilter publicRateLimitFilter;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final PublicSecurityExceptionHandler publicSecurityExceptionHandler;
    private final GoogleOAuth2LoginSuccessHandler googleOAuth2LoginSuccessHandler;
    private final GoogleOAuth2LoginFailureHandler googleOAuth2LoginFailureHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public PublicSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PublicRateLimitFilter publicRateLimitFilter,
            GoogleOAuthProperties googleOAuthProperties,
            PublicSecurityExceptionHandler publicSecurityExceptionHandler,
            @Autowired(required = false) GoogleOAuth2LoginSuccessHandler googleOAuth2LoginSuccessHandler,
            @Autowired(required = false) GoogleOAuth2LoginFailureHandler googleOAuth2LoginFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.publicRateLimitFilter = publicRateLimitFilter;
        this.googleOAuthProperties = googleOAuthProperties;
        this.publicSecurityExceptionHandler = publicSecurityExceptionHandler;
        this.googleOAuth2LoginSuccessHandler = googleOAuth2LoginSuccessHandler;
        this.googleOAuth2LoginFailureHandler = googleOAuth2LoginFailureHandler;
    }

    @Bean
    public SecurityFilterChain publicApiSecurityFilterChain(HttpSecurity http) throws Exception {
        boolean googleOAuthEnabled = googleOAuthProperties.isConfigured()
                && googleOAuth2LoginSuccessHandler != null
                && googleOAuth2LoginFailureHandler != null;

        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(
                googleOAuthEnabled ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(publicSecurityExceptionHandler)
                .accessDeniedHandler(publicSecurityExceptionHandler));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers(GOOGLE_OAUTH_PATHS).permitAll();
            auth.requestMatchers("/error").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/public/auth/logout").authenticated();
            auth.requestMatchers("/api/public/signalements/mine").authenticated();
            auth.requestMatchers(HttpMethod.POST, "/api/public/signalements/*/reponses").authenticated();
            auth.requestMatchers(
                    "/api/public/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health"
            ).permitAll();
            auth.anyRequest().denyAll();
        });

        if (googleOAuthEnabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository()))
                    .successHandler(googleOAuth2LoginSuccessHandler)
                    .failureHandler(googleOAuth2LoginFailureHandler));
        }

        http.addFilterBefore(publicRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, PublicRateLimitFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
