package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import lombok.Data;

import java.time.Instant;

/**
 * Critères de recherche multicritère pour les signalements.
 * Tous les champs sont optionnels.
 */
@Data
public class ReportCriteria {

    /** Filtre partiel sur la référence du signalement. */
    private String reference;

    /** Filtre partiel sur la description. */
    private String description;

    /** Filtre exact sur la priorité. */
    private Priority priority;

    /** Filtre exact sur l'identifiant du type de signalement. */
    private Long reportTypeId;

    /** Filtre exact sur l'identifiant du statut courant. */
    private Long statusId;

    /** Filtre exact sur l'UUID du support concerné. */
    private String supportUuid;

    /** Filtre partiel sur la référence du support concerné. */
    private String supportReference;

    /** Début de plage pour la date de création du signalement. */
    private Instant creationDateFrom;

    /** Fin de plage pour la date de création du signalement. */
    private Instant creationDateTo;

    /** Début de plage pour la date de clôture du signalement. */
    private Instant closureDateFrom;

    /** Fin de plage pour la date de clôture du signalement. */
    private Instant closureDateTo;
}
