package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Ligne de liste « Mes signalements » (voyageur authentifié).
 */
@Data
@Builder
public class PublicReportListItemResponse {

    private UUID uuid;
    private String reference;
    private Instant creationDate;
    private String supportLabel;
    private String supportTypeLabel;
    private String statusCode;
    private String statusLabel;
}
