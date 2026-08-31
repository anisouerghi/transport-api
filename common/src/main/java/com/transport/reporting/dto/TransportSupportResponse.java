package com.transport.reporting.dto;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO reponse support de transport (complet pour consultation admin).
 */
@Data
@Builder
public class TransportSupportResponse {

    /** Identifiant technique. */
    private Long transportSupportId;

    /** UUID public encode dans le QR Code. */
    private UUID uuid;

    /** Reference metier unique. */
    private String reference;

    /** Libelle descriptif. */
    private String label;

    /** URL publique de signalement : {app.qr.base-url}report/{uuid}. */
    private String qrCodeUrl;

    /** Chemin du fichier image QR sur le serveur. */
    private String qrCodePath;

    /** Date de generation du QR. */
    private Instant qrDateCreation;

    /** Date d'impression physique (si renseignee). */
    private Instant qrDateImpression;

    /** Statut du QR (GENERATED, PRINTED, ACTIVE, DISABLED). */
    private QrStatus qrStatus;

    /** Statut operationnel (ACTIVE, INACTIVE, MAINTENANCE). */
    private SupportStatus supportStatus;

    /** Id du type de support lie. */
    private Long supportTypeId;

    /** Code du type de support. */
    private String supportTypeCode;

    /** Libelle du type de support. */
    private String supportTypeLabel;

    /** Id du district. */
    private Long districtId;

    /** Code du district. */
    private String districtCode;

    /** Libelle du district. */
    private String districtLabel;

    /** Date de creation de l'enregistrement. */
    private Instant createdAt;

    /** Date de derniere modification. */
    private Instant updatedAt;

    /** Version optimistic lock. */
    private Long version;
}
