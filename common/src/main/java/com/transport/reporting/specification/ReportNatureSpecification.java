package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.ReportNatureCriteria;
import com.transport.reporting.entity.ReportNature;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ReportNatureSpecification {

    private ReportNatureSpecification() {
    }

    public static Specification<ReportNature> fromCriteria(ReportNatureCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria == null) {
                return cb.conjunction();
            }
            SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "code", criteria.getCode());
            SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "label", criteria.getLabel());
            SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "description", criteria.getDescription());
            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
