package com.transport.reporting.service;

import com.transport.reporting.dto.EmailSendResult;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Envoi d'e-mails HTML via {@link JavaMailSender}.
 * Expéditeur unique = {@code spring.mail.from} (reclamations@transtu.tn).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String LOGO_CLASSPATH = "email/transtu_logo.png";

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final int mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final String fromAddress;
    private final String fromName;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:0}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.from:${spring.mail.username:}}") String fromAddress,
            @Value("${app.mail.from-name:}") String fromName) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailPort = mailPort;
        this.mailUsername = mailUsername;
        this.mailPassword = stripOptionalQuotes(mailPassword);
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        log.info("SMTP config chargee: host={}, port={}, username={}, from={}, fromName={}, passwordLength={}",
                this.mailHost, this.mailPort, this.mailUsername, this.fromAddress, this.fromName,
                this.mailPassword != null ? this.mailPassword.length() : 0);
    }

    public List<String> missingConfigurationProperties() {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(mailHost)) {
            missing.add("spring.mail.host");
        }
        if (mailPort <= 0) {
            missing.add("spring.mail.port");
        }
        if (!StringUtils.hasText(mailUsername)) {
            missing.add("spring.mail.username");
        }
        if (!StringUtils.hasText(mailPassword)) {
            missing.add("spring.mail.password");
        }
        if (!StringUtils.hasText(fromAddress)) {
            missing.add("spring.mail.from (ou spring.mail.username)");
        }
        return missing;
    }

    public EmailSendResult sendHtml(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to)) {
            return EmailSendResult.fail("EMAIL_NO_RECIPIENT",
                    "Aucun destinataire e-mail n'est disponible pour cet envoi.");
        }

        List<String> missing = missingConfigurationProperties();
        if (!missing.isEmpty()) {
            String props = String.join(", ", missing);
            log.error("Configuration SMTP incomplète. Propriétés manquantes : {}", props);
            return EmailSendResult.fail("EMAIL_CONFIG_INCOMPLETE",
                    "Configuration SMTP incomplète. Propriétés manquantes : " + props + ".");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            applyFromAndReplyTo(helper);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setSentDate(new java.util.Date());
            helper.setText(htmlBody, true);
            attachInlineLogo(helper);
            mailSender.send(message);
            String messageId = message.getMessageID();
            log.info("E-mail accepté par SMTP from={} to={} host={} (Message-ID: {})",
                    fromAddress, to, mailHost, messageId != null ? messageId : "n/a");
            return EmailSendResult.ok("E-mail accepté par le serveur SMTP pour " + to.trim()
                    + ". Vérifiez la boîte de réception et les indésirables ; l'arrivée peut prendre quelques minutes.");
        } catch (Exception ex) {
            EmailSendResult mapped = mapException(ex);
            log.error("Échec envoi e-mail à {} [{}]: {}", to, mapped.getErrorCode(), ex.getMessage(), ex);
            return mapped;
        }
    }

    private void applyFromAndReplyTo(MimeMessageHelper helper)
            throws MessagingException, java.io.UnsupportedEncodingException {
        String from = fromAddress.trim();
        if (StringUtils.hasText(fromName)) {
            helper.setFrom(from, fromName.trim());
        } else {
            helper.setFrom(from);
        }
        helper.setReplyTo(from);
    }

    private void attachInlineLogo(MimeMessageHelper helper) {
        try {
            ClassPathResource logo = new ClassPathResource(LOGO_CLASSPATH);
            if (!logo.exists()) {
                log.warn("Logo e-mail introuvable sur le classpath : {}", LOGO_CLASSPATH);
                return;
            }
            helper.addInline(ReplyEmailComposer.LOGO_CONTENT_ID, logo, "image/png");
        } catch (Exception ex) {
            log.warn("Impossible d'attacher le logo TRANSTU à l'e-mail : {}", ex.getMessage());
        }
    }

    private EmailSendResult mapException(Throwable ex) {
        Throwable root = rootCause(ex);

        if (ex instanceof MailAuthenticationException || root instanceof AuthenticationFailedException) {
            return EmailSendResult.fail("EMAIL_SMTP_AUTH",
                    "L'e-mail n'a pas pu être envoyé : authentification SMTP refusée (identifiants invalides).");
        }
        if (root instanceof SocketTimeoutException) {
            return EmailSendResult.fail("EMAIL_SMTP_TIMEOUT",
                    "L'e-mail n'a pas pu être envoyé : le serveur SMTP n'a pas répondu à temps (timeout).");
        }
        if (root instanceof ConnectException) {
            return EmailSendResult.fail("EMAIL_SMTP_UNAVAILABLE",
                    "L'e-mail n'a pas pu être envoyé : serveur SMTP inaccessible (hôte ou port incorrect).");
        }
        String raw = safeDetail(root);
        String detail = raw.toLowerCase();

        if (ex instanceof MailSendException || root instanceof MessagingException) {
            if (detail.contains("authentication") || detail.contains("535") || detail.contains("auth fail")) {
                return EmailSendResult.fail("EMAIL_SMTP_AUTH",
                        "L'e-mail n'a pas pu être envoyé : authentification SMTP refusée (vérifiez username/password).");
            }
            if (detail.contains("ssl") || detail.contains("tls") || detail.contains("certificate")
                    || detail.contains("pkix") || detail.contains("handshake")) {
                return EmailSendResult.fail("EMAIL_SMTP_TLS",
                        "L'e-mail n'a pas pu être envoyé : problème TLS/SSL SMTP. Détail : " + raw);
            }
            if (detail.contains("timed out") || detail.contains("timeout")) {
                return EmailSendResult.fail("EMAIL_SMTP_TIMEOUT",
                        "L'e-mail n'a pas pu être envoyé : délai d'attente SMTP dépassé.");
            }
            if (detail.contains("unknown host") || detail.contains("unreachable") || detail.contains("connection refused")) {
                return EmailSendResult.fail("EMAIL_SMTP_UNAVAILABLE",
                        "L'e-mail n'a pas pu être envoyé : serveur SMTP inaccessible. Détail : " + raw);
            }
            return EmailSendResult.fail("EMAIL_SMTP_SEND",
                    "L'e-mail n'a pas pu être envoyé : erreur SMTP. Détail : " + raw);
        }
        return EmailSendResult.fail("EMAIL_SEND_FAILED",
                "L'e-mail n'a pas pu être envoyé. Détail : " + (raw.isEmpty() ? "erreur technique" : raw));
    }

    private static String safeDetail(Throwable root) {
        if (root == null || root.getMessage() == null) {
            return root != null ? root.getClass().getSimpleName() : "inconnu";
        }
        String msg = root.getMessage().replaceAll("(?i)password[=:].*", "password=***").trim();
        return msg.length() > 220 ? msg.substring(0, 220) + "…" : msg;
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
