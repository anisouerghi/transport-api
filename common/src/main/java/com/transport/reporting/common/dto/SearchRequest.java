package com.transport.reporting.common.dto;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Requete de recherche generique envoyee par le frontend.
 * <pre>
 * {
 *   "filters": { ... criteres metier ... },
 *   "pageable": { "page": 0, "size": 10, "sortBy": "code", "sortDirection": "ASC" }
 * }
 * </pre>
 *
 * @param &lt;F&gt; type des filtres (ex. SupportTypeCriteria, TransportSupportCriteria)
 */
@Data
public class SearchRequest<F> {

    /** Criteres de filtre (peut etre null = aucun filtre). */
    private F filters;

    /** Pagination et tri (defaut page 0, size 10). */
    @Valid
    private PageRequest pageable = new PageRequest();
}
