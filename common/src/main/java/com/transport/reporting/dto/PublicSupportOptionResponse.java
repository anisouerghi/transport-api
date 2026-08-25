package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Option publique minimale pour le choix d'un support (accès direct, sans QR).
 * N'expose pas les chemins fichiers ni les champs d'administration.
 */
@Data
@Builder
public class PublicSupportOptionResponse {

    private UUID uuid;
    private String reference;
    private String label;
    private Long supportTypeId;
    private String supportTypeCode;
    private String supportTypeLabel;
}
