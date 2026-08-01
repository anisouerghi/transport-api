package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue publique sécurisée d'un signalement (suivi voyageur par UUID).
 * N'expose pas les IDs internes, priorités, agents ni réponses privées.
 */
@Data
@Builder
public class PublicReportTrackingResponse {

    private UUID uuid;
    /** Référence métier (informativ uniquement). */
    private String reference;
    private Instant creationDate;
    private String description;
    private String reportTypeLabel;
    private String supportLabel;
    private String statusCode;
    private String statusLabel;
    private List<PublicReplyView> replies;

    @Data
    @Builder
    public static class PublicReplyView {
        private String message;
        private Instant replyDate;
    }
}
