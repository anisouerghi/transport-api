package com.transport.reporting.security.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Compteurs de rate limit en mémoire JVM (fenêtre fixe).
 * Les entrées disparaissent au redémarrage du processus.
 */
@Component
public class InMemoryRateLimiter {

    private static final int CLEANUP_THRESHOLD = 5_000;

    private final LongSupplier clockMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() {
        this(System::currentTimeMillis);
    }

    InMemoryRateLimiter(LongSupplier clockMillis) {
        this.clockMillis = clockMillis;
    }

    public RateLimitDecision consume(String key, int limit, int windowSeconds) {
        long now = clockMillis.getAsLong();
        long windowMs = windowSeconds * 1000L;
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.expiresAtMillis) {
                return new Window(now + windowMs, 1);
            }
            current.count++;
            return current;
        });
        maybeCleanup(now);
        if (window.count <= limit) {
            return RateLimitDecision.allow();
        }
        long remainingMs = Math.max(0, window.expiresAtMillis - now);
        int retryAfter = (int) Math.max(1, (remainingMs + 999) / 1000);
        return RateLimitDecision.deny(retryAfter);
    }

    int size() {
        return windows.size();
    }

    private void maybeCleanup(long now) {
        if (windows.size() < CLEANUP_THRESHOLD) {
            return;
        }
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Window> entry = it.next();
            if (now >= entry.getValue().expiresAtMillis) {
                it.remove();
            }
        }
    }

    private static final class Window {
        private final long expiresAtMillis;
        private int count;

        private Window(long expiresAtMillis, int count) {
            this.expiresAtMillis = expiresAtMillis;
            this.count = count;
        }
    }
}
