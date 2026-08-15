package com.transport.reporting.service;

import com.transport.reporting.config.FrontendProperties;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Compose le contenu HTML des e-mails de réponse agent → voyageur.
 */
@Component
public class ReplyEmailComposer {

    private static final Logger log = LoggerFactory.getLogger(ReplyEmailComposer.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(Locale.FRENCH);
    private static final String REPLY_TEMPLATE = "email/reply.html";
    private static final String CONFIRMATION_TEMPLATE = "email/confirmation.html";

    /** Content-ID de l'image logo embarquée dans l'e-mail (voir EmailService). */
    public static final String LOGO_CONTENT_ID = "transtu-logo";

    private final FrontendProperties frontendProperties;
    private final String replyTemplate;
    private final String confirmationTemplate;

    public ReplyEmailComposer(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
        this.replyTemplate = loadTemplate(REPLY_TEMPLATE);
        this.confirmationTemplate = loadTemplate(CONFIRMATION_TEMPLATE);
    }

    public String subject() {
        return "Réponse à votre signalement – TRANSTU";
    }

    public String confirmationSubject() {
        return "Votre signalement a bien été enregistré – TRANSTU";
    }

    public String buildHtml(Report report, Reply reply) {
        String trackingUrl = trackingUrl(report);
        String replyDate = reply.getReplyDate() != null
                ? DATE_FMT.format(reply.getReplyDate().atZone(ZoneId.systemDefault()))
                : "";
        String messageHtml = escapeHtml(reply.getMessage()).replace("\n", "<br/>");
        return replyTemplate
                .replace("{{LOGO_CID}}", LOGO_CONTENT_ID)
                .replace("{{REFERENCE}}", escapeHtml(report.getReference()))
                .replace("{{REPLY_DATE}}", escapeHtml(replyDate))
                .replace("{{MESSAGE}}", messageHtml)
                .replace("{{TRACKING_URL}}", trackingUrl);
    }

    public String buildConfirmationHtml(Report report) {
        String trackingUrl = trackingUrl(report);
        return confirmationTemplate
                .replace("{{LOGO_CID}}", LOGO_CONTENT_ID)
                .replace("{{REFERENCE}}", escapeHtml(report.getReference()))
                .replace("{{TRACKING_URL}}", trackingUrl);
    }

    private String trackingUrl(Report report) {
        if (report == null || report.getUuid() == null) {
            return frontendProperties.buildTrackingUrl("");
        }
        return frontendProperties.buildTrackingUrl(report.getUuid().toString());
    }

    private static String loadTemplate(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            try (java.io.InputStream in = resource.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            log.error("Template e-mail introuvable : {}", classpathLocation, ex);
            return "<html><body><p>{{REFERENCE}}</p><p>{{MESSAGE}}</p><p><a href=\"{{TRACKING_URL}}\">{{TRACKING_URL}}</a></p></body></html>";
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
