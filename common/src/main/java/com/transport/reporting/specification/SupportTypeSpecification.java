package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.SupportTypeCriteria;
import com.transport.reporting.entity.SupportType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications JPA pour SupportType.
 * Convertit {@link SupportTypeCriteria} en predicats Criteria API.
 */
public final class SupportTypeSpecification {

    private SupportTypeSpecification() {
    }

    /**
     * Construit une Specification a partir des criteres (tous optionnels).
     * Si criteria est null, retourne une spec sans filtre (tous les enregistrements).
     */
    public static Specification<SupportType> fromCriteria(SupportTypeCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "code", criteria.getCode());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "label", criteria.getLabel());
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
