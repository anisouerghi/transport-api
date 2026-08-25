package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications JPA pour les signalements.
 * Convertit {@link ReportCriteria} en predicates Criteria API.
 */
public final class ReportSpecification {

    private ReportSpecification() {
    }

    public static Specification<Report> fromCriteria(ReportCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

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
                        "%" + criteria.getSupportReference().trim().toLowerCase() + "%"
                ));
            }

            SpecificationUtils.addInstantRange(predicates, cb, root, "creationDate",
                    criteria.getCreationDateFrom(), criteria.getCreationDateTo());
            SpecificationUtils.addInstantRange(predicates, cb, root, "closureDate",
                    criteria.getClosureDateFrom(), criteria.getClosureDateTo());

            if (criteria.getReplied() != null) {
                Subquery<Long> replyExists = query.subquery(Long.class);
                Root<Reply> replyRoot = replyExists.from(Reply.class);
                replyExists.select(replyRoot.get("replyId"));
                replyExists.where(cb.equal(replyRoot.get("report").get("reportId"), root.get("reportId")));
                if (Boolean.TRUE.equals(criteria.getReplied())) {
                    predicates.add(cb.exists(replyExists));
                } else {
                    predicates.add(cb.not(cb.exists(replyExists)));
                }
            }

            if (Boolean.TRUE.equals(criteria.getUncategorized())) {
                predicates.add(cb.isNull(root.get("nature")));
            } else if (criteria.getNatureId() != null) {
                predicates.add(cb.equal(root.join("nature").get("reportNatureId"), criteria.getNatureId()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
