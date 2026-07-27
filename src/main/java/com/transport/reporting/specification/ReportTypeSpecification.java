package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.ReportTypeCriteria;
import com.transport.reporting.entity.ReportType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications JPA pour ReportType.
 */
public final class ReportTypeSpecification {

    private ReportTypeSpecification() {
    }

    public static Specification<ReportType> fromCriteria(ReportTypeCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "code", criteria.getCode());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "label", criteria.getLabel());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "description", criteria.getDescription());
                if (criteria.getActive() != null) {
                    predicates.add(cb.equal(root.get("active"), criteria.getActive()));
                }
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
