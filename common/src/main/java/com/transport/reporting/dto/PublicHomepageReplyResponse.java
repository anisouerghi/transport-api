package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Réponse publiée sur l'accueil voyageur.
 * Aucune donnée personnelle ni identifiant de dossier.
 */
@Data
@Builder
public class PublicHomepageReplyResponse {

    private String message;
    private Instant replyDate;
    private String reportTypeLabel;
}
