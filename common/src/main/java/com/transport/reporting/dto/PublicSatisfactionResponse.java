package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** Avis enregistré, sans donnée personnelle du voyageur. */
@Data
@Builder
public class PublicSatisfactionResponse {

    private String level;
    private String comment;
    private Instant createdAt;
}
