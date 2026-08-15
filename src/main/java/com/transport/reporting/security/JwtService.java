package com.transport.reporting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Génération et validation JWT (HS256).
 * Deux familles : {@link #TYPE_ADMIN} (back-office) et {@link #TYPE_PASSENGER} (voyageur).
 */
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_PASSENGER = "PASSENGER";
    public static final String TYPE_ADMIN = "ADMIN";

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes = decodeSecret(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_TYPE, TYPE_ADMIN)
                .claim("uid", principal.getUserId())
                .claim("name", principal.getFullName())
                .claim("roles", List.copyOf(principal.getRoles()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generatePassengerToken(PassengerPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.getEmail())
                .claim(CLAIM_TYPE, TYPE_PASSENGER)
                .claim("pid", principal.getPassengerId())
                .claim("name", principal.getName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        Object typ = parseClaims(token).get(CLAIM_TYPE);
        return typ != null ? typ.toString() : TYPE_ADMIN;
    }

    public Long extractPassengerId(String token) {
        Object pid = parseClaims(token).get("pid");
        if (pid instanceof Number) {
            return ((Number) pid).longValue();
        }
        return pid != null ? Long.valueOf(pid.toString()) : null;
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        String username = extractUsername(token);
        return username.equals(principal.getUsername()) && !isExpired(token);
    }

    public boolean isPassengerTokenValid(String token, PassengerPrincipal principal) {
        if (!TYPE_PASSENGER.equals(extractTokenType(token)) || isExpired(token)) {
            return false;
        }
        Long pid = extractPassengerId(token);
        return pid != null && pid.equals(principal.getPassengerId());
    }

    public boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (Exception ignored) {
            // fallback UTF-8
        }
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least 256 bits (32 bytes) or Base64-encoded equivalent");
        }
        return raw;
    }
}
