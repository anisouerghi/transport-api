package com.transport.reporting.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Reponse paginee generique renvoyee au frontend.
 * Correspond a la structure {@code PageResult} cote Angular.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** Elements de la page courante. */
    private List<T> content;

    /** Nombre total d'elements (toutes pages confondues). */
    private long totalElements;

    /** Nombre total de pages. */
    private int totalPages;

    /** Index de la page courante (0-based). */
    private int page;

    /** Taille de page demandee. */
    private int size;

    /** Convertit une {@link Page} Spring Data en PageResponse. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
