package com.transport.reporting.modules.support.dto;

import com.transport.reporting.common.enums.TypeSupport;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SupportResponse {

    private Long id;
    private UUID uuid;
    private String reference;
    private String libelle;
    private TypeSupport type;
    private String qrCodeUrl;
    private Instant qrDateCreation;
    private boolean actif;
}
