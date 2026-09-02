package com.transport.reporting.security;

import com.transport.reporting.dto.PassengerAuthResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage éphémère des codes d'échange post-OAuth (évite d'exposer le JWT dans l'URL).
 */
@Component
public class GoogleOAuthCallbackCodeStore {

    private static final long TTL_SECONDS = 300;

    private record Entry(PassengerAuthResponse authResponse, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public String issue(PassengerAuthResponse authResponse) {
        purgeExpired();
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(authResponse, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    public Optional<PassengerAuthResponse> redeem(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        Entry entry = codes.remove(code.trim());
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.authResponse());
    }

    private void purgeExpired() {
        codes.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
