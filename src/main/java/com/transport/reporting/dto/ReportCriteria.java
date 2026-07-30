package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import lombok.Data;

import java.time.Instant;

/**
 * Criteres de recherche multicritere pour les signalements.
 * Utilises par POST /api/admin/signalements/search.
 * Tous les champs sont optionnels.
 */
@Data
public class ReportCriteria {

    /** Filtre partiel sur la reference (LIKE). */
    private String reference;

    /** Filtre partiel sur la description (LIKE). */
    private String description;

    /** Filtre exact sur la priorite. */
    private Priority priority;

    /** Filtre exact sur l'identifiant du type de signalement. */
    private Long reportTypeId;

    /** Filtre exact sur l'identifiant du statut. */
    private Long statusId;

    /** Filtre exact sur l'UUID du support de transport. */
    private String supportUuid;

    /** Filtre partiel sur la reference du support (LIKE). */
    private String supportReference;

    /** Debut de plage pour creationDate (inclus). */
    private Instant creationDateFrom;

    /** Fin de plage pour creationDate (inclus). */
    private Instant creationDateTo;

    /** Debut de plage pour closureDate (inclus). */
    private Instant closureDateFrom;

    /** Fin de plage pour closureDate (inclus). */
    private Instant closureDateTo;

    /** Filtre partiel sur le code du type de signalement (LIKE). */
    private String reportType;

    /** Filtre partiel sur le code du statut (LIKE). */
    private String status;
}
