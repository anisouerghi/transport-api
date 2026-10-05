package com.transport.reporting.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.reporting.config.RateLimitProperties;
import com.transport.reporting.config.RateLimitProperties.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Limite les API publiques avant les contrôleurs.
 * Inactif lorsque {@code app.rate-limit.enabled=false}.
 */
@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublicRateLimitFilter.class);

    private final RateLimitProperties properties;
    private final InMemoryRateLimiter limiter;
    private final ObjectMapper objectMapper;

    public PublicRateLimitFilter(
            RateLimitProperties properties,
            InMemoryRateLimiter limiter,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.limiter = limiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = path(request);
        String method = request.getMethod();
        Rule rule = match(method, path);
        if (rule == null || !rule.bucket().isActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest current = request;
        String subject = "";
        if (rule.withSubject()) {
            CachedBodyRequest cached = new CachedBodyRequest(request);
            current = cached;
            subject = subjectHash(cached.body(), rule.subjectField());
        }

        String key = rule.name() + ":" + clientIp(request) + (subject.isEmpty() ? "" : ":" + subject);
        RateLimitDecision decision = limiter.consume(key, rule.bucket().getLimit(), rule.bucket().getWindowSeconds());
        if (!decision.allowed()) {
            log.warn("Rate limit {} dépassé, retryAfter={}s", rule.name(), decision.retryAfterSeconds());
            RateLimitResponses.write(objectMapper, request, response, decision.retryAfterSeconds());
            return;
        }
        filterChain.doFilter(current, response);
    }

    private Rule match(String method, String path) {
        if (HttpMethod.POST.matches(method) && "/api/public/signalements".equals(path)) {
            return Rule.ip("signalement", properties.getSignalement());
        }
        if (HttpMethod.POST.matches(method)
                && ("/api/public/auth/login".equals(path) || "/api/public/auth/register".equals(path))) {
            return Rule.subject("otp", properties.getOtp(), "email");
        }
        if (HttpMethod.POST.matches(method) && "/api/public/auth/otp/resend".equals(path)) {
            return Rule.subject("otp", properties.getOtp(), "otpTransactionId");
        }
        if (HttpMethod.POST.matches(method) && "/api/public/auth/otp/verify".equals(path)) {
            return Rule.subject("otp-verify", properties.getOtpVerify(), "otpTransactionId");
        }
        if (HttpMethod.GET.matches(method)
                && (path.startsWith("/api/public/suivi/")
                || path.matches("/api/public/signalements/[^/]+/follow-up")
                || path.matches("/api/public/signalements/reference/[^/]+"))) {
            // Même bucket que le suivi par UUID : la référence est devinable,
            // ce point d'entrée public ne doit pas être plus permissif que le lien e-mail.
            return Rule.ip("suivi", properties.getSuivi());
        }
        if (HttpMethod.GET.matches(method) && "/api/public/reponses".equals(path)) {
            return Rule.ip("reponses", properties.getReponses());
        }
        if (HttpMethod.GET.matches(method)
                && ("/api/public/report-types".equals(path) || path.startsWith("/api/public/supports"))) {
            return Rule.ip("catalog", properties.getCatalog());
        }
        return null;
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    static String clientIp(HttpServletRequest request) {
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (StringUtils.hasText(cloudflare)) {
            return cloudflare.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "0.0.0.0";
    }

    private String subjectHash(byte[] body, String field) {
        if (body == null || body.length == 0) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode value = node.get(field);
            if (value == null || !value.isTextual() || !StringUtils.hasText(value.asText())) {
                return "";
            }
            return sha256(value.asText().trim().toLowerCase());
        } catch (Exception ex) {
            return "";
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private record Rule(String name, Bucket bucket, boolean withSubject, String subjectField) {
        static Rule ip(String name, Bucket bucket) {
            return new Rule(name, bucket, false, null);
        }

        static Rule subject(String name, Bucket bucket, String field) {
            return new Rule(name, bucket, true, field);
        }
    }
}
