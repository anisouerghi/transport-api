package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Réponse publiée sur l'accueil voyageur.
 */
@Data
@Builder
public class PublicHomepageReplyResponse {

    private String description;
    private String message;
    private String passengerName;
    private Instant replyDate;
    private String reportTypeLabel;
}
