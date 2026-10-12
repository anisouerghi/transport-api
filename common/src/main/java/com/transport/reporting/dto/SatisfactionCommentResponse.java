package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** Commentaire d'avis visible par un agent habilité, sans identité du voyageur. */
@Data
@Builder
public class SatisfactionCommentResponse {

    private String reference;
    private String reportTypeLabel;
    private String supportLabel;
    private String level;
    private String comment;
    private Instant createdAt;
}
