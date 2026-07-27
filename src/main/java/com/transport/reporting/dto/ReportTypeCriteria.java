package com.transport.reporting.dto;

import lombok.Data;

/**
 * Criteres de recherche multicritere pour ReportType.
 */
@Data
public class ReportTypeCriteria {

    /** Filtre partiel sur le code (LIKE). */
    private String code;

    /** Filtre partiel sur le libelle (LIKE). */
    private String label;

    /** Filtre partiel sur la description (LIKE). */
    private String description;

    /** Filtre exact sur le statut actif (true / false). */
    private Boolean active;
}
