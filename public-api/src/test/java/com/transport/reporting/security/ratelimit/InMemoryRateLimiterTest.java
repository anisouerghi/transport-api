package com.transport.reporting.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    private long now;
    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        now = 1_000_000L;
        limiter = new InMemoryRateLimiter(() -> now);
    }

    @Test
    void allowsUpToTheLimitThenRejects() {
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.consume("signalement:1.1.1.1", 5, 600).allowed());
        }
        RateLimitDecision blocked = limiter.consume("signalement:1.1.1.1", 5, 600);
        assertFalse(blocked.allowed());
        assertTrue(blocked.retryAfterSeconds() > 0);
    }

    @Test
    void resetsAfterTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.consume("otp:1.1.1.1:abc", 3, 600);
        }
        assertFalse(limiter.consume("otp:1.1.1.1:abc", 3, 600).allowed());

        now += 600_000L;
        assertTrue(limiter.consume("otp:1.1.1.1:abc", 3, 600).allowed());
    }

    @Test
    void isolatesDifferentKeys() {
        for (int i = 0; i < 3; i++) {
            limiter.consume("otp:1.1.1.1:abc", 3, 600);
        }
        assertFalse(limiter.consume("otp:1.1.1.1:abc", 3, 600).allowed());
        assertTrue(limiter.consume("otp:2.2.2.2:abc", 3, 600).allowed());
        assertTrue(limiter.consume("suivi:1.1.1.1", 60, 60).allowed());
    }
}
