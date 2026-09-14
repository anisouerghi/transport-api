package com.transport.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.reporting.common.util.RequestMetadata;
import com.transport.reporting.config.CloudflareProperties;
import com.transport.reporting.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Validation serveur d'un token Cloudflare Turnstile via l'API siteverify.
 * La secret key ne quitte jamais le backend.
 */
@Service
public class TurnstileValidationService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileValidationService.class);
    private static final URI SITEVERIFY_URI =
            URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify");

    private final CloudflareProperties cloudflareProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TurnstileValidationService(CloudflareProperties cloudflareProperties, ObjectMapper objectMapper) {
        this.cloudflareProperties = cloudflareProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Si Turnstile est désactivé : no-op.
     * Si activé : exige un token et le vérifie auprès de Cloudflare.
     */
    public void verifyOrThrow(String turnstileToken) {
        if (!cloudflareProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(cloudflareProperties.getSecretKey())) {
            throw new BusinessException(
                    "La vérification de sécurité est temporairement indisponible. Réessayez plus tard.",
                    "TURNSTILE_MISCONFIGURED");
        }
        if (!StringUtils.hasText(turnstileToken)) {
            throw new BusinessException(
                    "Veuillez effectuer la vérification de sécurité avant d'envoyer votre signalement.",
                    "TURNSTILE_REQUIRED");
        }

        try {
            String body = "secret=" + urlEncode(cloudflareProperties.getSecretKey().trim())
                    + "&response=" + urlEncode(turnstileToken.trim());
            String ip = RequestMetadata.currentIpAddress();
            if (StringUtils.hasText(ip) && !"0.0.0.0".equals(ip)) {
                body += "&remoteip=" + urlEncode(ip);
            }

            HttpRequest request = HttpRequest.newBuilder(SITEVERIFY_URI)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Turnstile siteverify HTTP {}", response.statusCode());
                throw new BusinessException(
                        "La vérification de sécurité a échoué. Veuillez réessayer.",
                        "TURNSTILE_HTTP_ERROR");
            }

            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("success").asBoolean(false)) {
                return;
            }

            String codes = json.path("error-codes").toString();
            log.info("Turnstile rejeté : {}", codes);
            throw new BusinessException(
                    "Vérification de sécurité invalide ou expirée. Veuillez la refaire puis renvoyer.",
                    "TURNSTILE_INVALID");
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    "La vérification de sécurité a été interrompue. Veuillez réessayer.",
                    "TURNSTILE_INTERRUPTED");
        } catch (Exception e) {
            log.warn("Erreur appel Turnstile siteverify : {}", e.getMessage());
            throw new BusinessException(
                    "Impossible de contacter le service de vérification. Veuillez réessayer.",
                    "TURNSTILE_UNAVAILABLE");
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
