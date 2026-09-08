package com.transport.reporting.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Révocation des JWT avant leur expiration naturelle.
 * Le stockage est local à l'instance; les tokens expirés sont supprimés à la lecture.
 */
@Service
public class RevokedTokenService {

    private final JwtService jwtService;
    private final ConcurrentMap<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public RevokedTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void revoke(String token) {
        long expiration = jwtService.parseClaims(token).getExpiration().getTime();
        if (expiration > System.currentTimeMillis()) {
            revokedTokens.put(token, expiration);
        }
    }

    public boolean isRevoked(String token) {
        Long expiration = revokedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration <= System.currentTimeMillis()) {
            revokedTokens.remove(token, expiration);
            return false;
        }
        return true;
    }
}