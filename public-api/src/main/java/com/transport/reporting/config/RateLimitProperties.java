package com.transport.reporting.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Limites des API publiques ({@code app.rate-limit.*}).
 * Surchargeables par arguments {@code java -jar} ou variables d'environnement.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private static final Logger log = LoggerFactory.getLogger(RateLimitProperties.class);

    private boolean enabled = true;
    private Bucket signalement = new Bucket(5, 600);
    private Bucket otp = new Bucket(3, 600);
    private Bucket otpVerify = new Bucket(5, 600);
    private Bucket suivi = new Bucket(60, 60);
    private Bucket reponses = new Bucket(60, 60);
    private Bucket catalog = new Bucket(120, 60);

    @PostConstruct
    void logConfig() {
        log.info(
                "Rate limit public-api : enabled={}, signalement={}/{}s, otp={}/{}s, otpVerify={}/{}s, suivi={}/{}s, reponses={}/{}s, catalog={}/{}s",
                enabled,
                signalement.limit, signalement.windowSeconds,
                otp.limit, otp.windowSeconds,
                otpVerify.limit, otpVerify.windowSeconds,
                suivi.limit, suivi.windowSeconds,
                reponses.limit, reponses.windowSeconds,
                catalog.limit, catalog.windowSeconds);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Bucket getSignalement() {
        return signalement;
    }

    public void setSignalement(Bucket signalement) {
        this.signalement = signalement;
    }

    public Bucket getOtp() {
        return otp;
    }

    public void setOtp(Bucket otp) {
        this.otp = otp;
    }

    public Bucket getOtpVerify() {
        return otpVerify;
    }

    public void setOtpVerify(Bucket otpVerify) {
        this.otpVerify = otpVerify;
    }

    public Bucket getSuivi() {
        return suivi;
    }

    public void setSuivi(Bucket suivi) {
        this.suivi = suivi;
    }

    public Bucket getReponses() {
        return reponses;
    }

    public void setReponses(Bucket reponses) {
        this.reponses = reponses;
    }

    public Bucket getCatalog() {
        return catalog;
    }

    public void setCatalog(Bucket catalog) {
        this.catalog = catalog;
    }

    public static class Bucket {
        private int limit = 10;
        private int windowSeconds = 60;

        public Bucket() {
        }

        public Bucket(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public boolean isActive() {
            return limit > 0 && windowSeconds > 0;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
