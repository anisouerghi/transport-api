package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.DistrictCriteria;
import com.transport.reporting.entity.District;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications JPA pour District.
 * Convertit {@link DistrictCriteria} en predicats Criteria API.
 */
public final class DistrictSpecification {

    private DistrictSpecification() {
    }

    /**
     * Construit une Specification a partir des criteres (tous optionnels).
     * Si criteria est null, retourne une spec sans filtre (tous les enregistrements).
     */
    public static Specification<District> fromCriteria(DistrictCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "codeDistrict", criteria.getCodeDistrict());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "libelleDistrict", criteria.getLibelleDistrict());
                SpecificationUtils.addEqual(predicates, cb, root, "etat", criteria.getEtat());
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
