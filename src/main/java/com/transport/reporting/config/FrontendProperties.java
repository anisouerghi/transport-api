package com.transport.reporting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URLs du frontend public voyageur ({@code app.frontend.*}).
 */
@Component
@ConfigurationProperties(prefix = "app.frontend")
@Getter
@Setter
public class FrontendProperties {

    /**
     * Base URL de l'application publique (sans slash final).
     * Ex. {@code http://localhost:4200} ou {@code https://signalement.transport.tn}.
     * Utilisée pour construire les liens de suivi sécurisés : {@code {base}/suivi/{uuid}}.
     */
    private String publicBaseUrl = "http://localhost:4200";

    /** URL absolue de suivi pour un signalement (UUID). */
    public String buildTrackingUrl(String reportUuid) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/suivi/" + reportUuid;
    }

    /** URL absolue du logo TRANSTU servi par le frontend public. */
    public String buildLogoUrl() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/assets/images/transtu_logo.png";
    }
}
