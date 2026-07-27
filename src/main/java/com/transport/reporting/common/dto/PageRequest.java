package com.transport.reporting.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Parametres de pagination et tri communs a toutes les recherches.
 * Inclus dans {@link SearchRequest#getPageable()}.
 */
@Data
public class PageRequest {

    /** Index de page (0 = premiere page). */
    @Min(0)
    private int page = 0;

    /** Nombre d'elements par page (max 100). */
    @Min(1)
    @Max(100)
    private int size = 10;

    /**
     * Nom logique du champ de tri (ex. "code", "reference").
     * Mappe cote service vers le vrai attribut JPA via SORT_FIELDS.
     */
    private String sortBy;

    /** Sens du tri : ASC ou DESC (defaut ASC). */
    private String sortDirection = "ASC";
}
