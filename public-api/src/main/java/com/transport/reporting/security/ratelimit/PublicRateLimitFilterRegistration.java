package com.transport.reporting.security.ratelimit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Empêche l'enregistrement servlet automatique : le filtre est branché
 * une seule fois dans la chaîne Spring Security.
 */
@Configuration
public class PublicRateLimitFilterRegistration {

    @Bean
    FilterRegistrationBean<PublicRateLimitFilter> disablePublicRateLimitServletRegistration(PublicRateLimitFilter filter) {
        FilterRegistrationBean<PublicRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
