package com.transport.reporting.common.util;

import com.transport.reporting.common.dto.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Utilitaires de conversion {@link PageRequest} (DTO API) vers Spring {@link Pageable}.
 * <p>
 * Securite : seul un champ present dans {@code allowedSortFields} est accepte
 * (evite les injections de noms de colonnes arbitraires).
 */
public final class PageableUtils {

    private PageableUtils() {
    }

    /**
     * @param request          parametres page/size/sort provenant du frontend (peut etre null)
     * @param defaultSortField attribut JPA utilise si sortBy est absent ou invalide
     * @param allowedSortFields map "nom logique frontend" -> "attribut JPA"
     */
    public static Pageable toPageable(PageRequest request, String defaultSortField,
                                      Map<String, String> allowedSortFields) {
        PageRequest pageRequest = request != null ? request : new PageRequest();
        String sortField = defaultSortField;
        if (pageRequest.getSortBy() != null && allowedSortFields.containsKey(pageRequest.getSortBy())) {
            sortField = allowedSortFields.get(pageRequest.getSortBy());
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(pageRequest.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getSize(),
                Sort.by(direction, sortField)
        );
    }
}
