package com.transport.reporting.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Configuration Cloudflare Turnstile ({@code app.cloudflare.*}).
 * <p>
 * Activé par défaut. La {@code secret-key} est injectée uniquement via
 * {@code CLOUDFLARE_SECRET_KEY} (jamais exposée au frontend ni commitée en PROD).
 * <p>
 * Cette classe bind la configuration Turnstile. La validation du token est déléguée
 * à {@code TurnstileValidationService}.
 */
@Component
@ConfigurationProperties(prefix = "app.cloudflare")
public class CloudflareProperties {

    private static final Logger log = LoggerFactory.getLogger(CloudflareProperties.class);

    /** Activé par défaut (DEV et PROD) ; désactivable via {@code CLOUDFLARE_ENABLED=false}. */
    private boolean enabled = true;
    private String siteKey = "";
    private String secretKey = "";

    @PostConstruct
    void logStatus() {
        log.info(
                "Cloudflare Turnstile : enabled={}, siteKeyConfigured={}, secretKeyConfigured={}",
                enabled,
                StringUtils.hasText(siteKey),
                StringUtils.hasText(secretKey));
        if (enabled && !StringUtils.hasText(secretKey)) {
            log.warn(
                    "Cloudflare active mais CLOUDFLARE_SECRET_KEY absente — la validation serveur ne pourra pas fonctionner.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey != null ? siteKey : "";
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey != null ? secretKey : "";
    }

    /** {@code true} si Turnstile est activé et que les clés site + secret sont présentes. */
    public boolean isConfigured() {
        return enabled && StringUtils.hasText(siteKey) && StringUtils.hasText(secretKey);
    }
}
