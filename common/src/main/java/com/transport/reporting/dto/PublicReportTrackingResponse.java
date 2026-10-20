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
    /** Conservé pour filtres / compatibilité ; ne pas afficher tel quel au voyageur. */
    private String statusCode;
    /**
     * Libellé public voyageur (ex. « En cours de traitement »).
     * Remplace l'affichage du statut interne agent.
     */
    private String statusLabel;
    /** Même valeur que {@link #statusLabel}, explicite pour les clients récents. */
    private String publicStatusLabel;
    /**
     * Vrai lorsqu'un cycle de complément est ouvert et que le signalement
     * n'est pas clôturé. Calculé par le serveur, indépendamment du client.
     */
    private boolean canPassengerReply;
    /** Le propriétaire connecté peut ajouter un commentaire tant que le dossier n'est pas clôturé. */
    private boolean canPassengerComment;
    /** Avis déjà enregistré. Sur une recherche par référence, réservé au propriétaire connecté. */
    private PublicSatisfactionResponse satisfaction;
    /** Le visiteur du lien UUID peut encore envoyer l'avis unique. */
    private boolean canSubmitSatisfaction;
    /** Dossier avec compte : le propriétaire doit se connecter. */
    private boolean satisfactionLoginRequired;
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
