package com.transport.reporting.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration OTP e-mail pour l'authentification voyageur ({@code app.auth.otp.*}).
 * Activé par défaut ; désactivable via {@code APP_AUTH_OTP_ENABLED=false}.
 */
@Component
@ConfigurationProperties(prefix = "app.auth.otp")
public class OtpProperties {

    private boolean enabled = true;
    private int length = 6;
    private int expirationMinutes = 5;
    private int maxAttempts = 5;
    private int resendDelaySeconds = 60;
    private int maxResendsPerChallenge = 5;

    @PostConstruct
    void validate() {
        if (length < 4 || length > 8) {
            throw new IllegalStateException("app.auth.otp.length must be between 4 and 8");
        }
        if (expirationMinutes < 1 || expirationMinutes > 60) {
            throw new IllegalStateException("app.auth.otp.expiration-minutes must be between 1 and 60");
        }
        if (maxAttempts < 1 || maxAttempts > 20) {
            throw new IllegalStateException("app.auth.otp.max-attempts must be between 1 and 20");
        }
        if (resendDelaySeconds < 10 || resendDelaySeconds > 600) {
            throw new IllegalStateException("app.auth.otp.resend-delay-seconds must be between 10 and 600");
        }
        if (maxResendsPerChallenge < 1 || maxResendsPerChallenge > 20) {
            throw new IllegalStateException("app.auth.otp.max-resends-per-challenge must be between 1 and 20");
        }
        org.slf4j.LoggerFactory.getLogger(OtpProperties.class).info(
                "OTP e-mail voyageur : enabled={}, length={}, expirationMinutes={}, maxAttempts={}, resendDelaySeconds={}",
                enabled, length, expirationMinutes, maxAttempts, resendDelaySeconds);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getResendDelaySeconds() {
        return resendDelaySeconds;
    }

    public void setResendDelaySeconds(int resendDelaySeconds) {
        this.resendDelaySeconds = resendDelaySeconds;
    }

    public int getMaxResendsPerChallenge() {
        return maxResendsPerChallenge;
    }

    public void setMaxResendsPerChallenge(int maxResendsPerChallenge) {
        this.maxResendsPerChallenge = maxResendsPerChallenge;
    }
}
