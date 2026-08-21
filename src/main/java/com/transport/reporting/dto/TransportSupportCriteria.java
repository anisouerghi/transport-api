package com.transport.reporting.dto;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Criteres de recherche multicritere pour TransportSupport.
 * Utilises par POST /api/admin/transport-supports/search.
 * Tous les champs sont optionnels.
 */
@Data
public class TransportSupportCriteria {

    /** Filtre partiel sur la reference (LIKE). */
    private String reference;

    /** Filtre partiel sur le libelle (LIKE). */
    private String label;

    /** Filtre exact sur l'UUID (chaine UUID valide). */
    private String uuid;

    /** Filtre exact sur le statut du QR Code. */
    private QrStatus qrStatus;

    /** Filtre exact sur le statut operationnel du support. */
    private SupportStatus supportStatus;

    /** Filtre exact sur l'identifiant du type de support. */
    private Long supportTypeId;

    /** Filtre exact sur l'identifiant du district. */
    private Long districtId;

    /** Debut de plage pour qrDateCreation (inclus). */
    private Instant qrDateCreationFrom;

    /** Fin de plage pour qrDateCreation (inclus). */
    private Instant qrDateCreationTo;

    /** Debut de plage pour qrDateImpression (inclus). */
    private Instant qrDateImpressionFrom;

    /** Fin de plage pour qrDateImpression (inclus). */
    private Instant qrDateImpressionTo;

    /** Debut de plage pour createdAt (inclus). */
    private Instant createdAtFrom;

    /** Fin de plage pour createdAt (inclus). */
    private Instant createdAtTo;
}
