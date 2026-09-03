package com.transport.reporting.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Compose le contenu HTML des e-mails OTP de connexion voyageur.
 */
@Component
public class OtpEmailComposer {

    private static final String OTP_TEMPLATE = "email/otp-verification.html";
    public static final String SUBJECT = "Votre code de vérification – TRANSTU";

    private final String otpTemplate;

    public OtpEmailComposer() {
        this.otpTemplate = loadTemplate(OTP_TEMPLATE);
    }

    public String buildHtml(String otpCode, int expirationMinutes) {
        return otpTemplate
                .replace("{{LOGO_CID}}", ReplyEmailComposer.LOGO_CONTENT_ID)
                .replace("{{OTP_CODE}}", escapeHtml(otpCode))
                .replace("{{EXPIRATION_MINUTES}}", String.valueOf(expirationMinutes));
    }

    private static String loadTemplate(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Modèle e-mail OTP introuvable : " + classpathLocation, ex);
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
