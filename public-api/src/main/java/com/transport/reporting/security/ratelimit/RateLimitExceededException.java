package com.transport.reporting.security.ratelimit;

/**
 * Levée lorsqu'une API publique dépasse sa fenêtre de requêtes.
 */
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super("Trop de requêtes. Veuillez réessayer plus tard.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
