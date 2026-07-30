package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.entity.Report;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Specifications JPA pour Report (signalement).
 * Convertit {@link ReportCriteria} en predicats Criteria API.
 */
public final class ReportSpecification {

    private ReportSpecification() {
    }

    public static Specification<Report> fromCriteria(ReportCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "reference", criteria.getReference());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "description", criteria.getDescription());
                SpecificationUtils.addEnumEqual(predicates, cb, root, "priority", criteria.getPriority());

                if (criteria.getReportTypeId() != null) {
                    predicates.add(cb.equal(root.join("reportType").get("reportTypeId"), criteria.getReportTypeId()));
                }
                if (criteria.getStatusId() != null) {
                    predicates.add(cb.equal(root.join("status").get("statusId"), criteria.getStatusId()));
                }

                if (criteria.getSupportUuid() != null && !criteria.getSupportUuid().isBlank()) {
                    try {
                        UUID uuid = UUID.fromString(criteria.getSupportUuid().trim());
                        predicates.add(cb.equal(root.join("transportSupport").get("uuid"), uuid));
                    } catch (IllegalArgumentException ignored) {
                        predicates.add(cb.disjunction());
                    }
                }
                if (criteria.getSupportReference() != null && !criteria.getSupportReference().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.join("transportSupport").get("reference")),
                            "%" + criteria.getSupportReference().trim().toLowerCase(Locale.ROOT) + "%"));
                }

                if (criteria.getReportType() != null && !criteria.getReportType().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.join("reportType").get("code")),
                            "%" + criteria.getReportType().trim().toLowerCase(Locale.ROOT) + "%"));
                }
                if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.join("status").get("code")),
                            "%" + criteria.getStatus().trim().toLowerCase(Locale.ROOT) + "%"));
                }

                SpecificationUtils.addInstantRange(predicates, cb, root, "creationDate",
                        criteria.getCreationDateFrom(), criteria.getCreationDateTo());
                SpecificationUtils.addInstantRange(predicates, cb, root, "closureDate",
                        criteria.getClosureDateFrom(), criteria.getClosureDateTo());
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
