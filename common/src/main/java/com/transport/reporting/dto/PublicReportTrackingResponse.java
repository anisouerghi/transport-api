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
    /** Heure serveur enregistrée à la clôture (RESOLVED ou CLOSED). Null si le dossier est ouvert. */
    private Instant closureDate;
    /**
     * Secondes entre {@code creationDate} et {@code closureDate}.
     * Null si le dossier n'est pas clôturé, si la date de clôture manque, ou si elle précède le dépôt.
     */
    private Long processingDurationSeconds;
    private String description;
    private String reportTypeLabel;
    private String supportLabel;
    private String statusCode;
    private String statusLabel;
    /**
     * Vrai lorsqu'un cycle de complément est ouvert et que le signalement
     * n'est pas clôturé. Calculé par le serveur, indépendamment du client.
     */
    private boolean canPassengerReply;
    private List<PublicReplyView> replies;

    @Data
    @Builder
    public static class PublicReplyView {
        private String message;
        private Instant replyDate;
        /** {@link com.transport.reporting.common.enums.ReplyType}. */
        private String replyType;
        /** {@link com.transport.reporting.common.enums.ReplyAuthorType}. */
        private String authorType;
    }
}
