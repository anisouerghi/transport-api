package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.AuditLogCriteria;
import com.transport.reporting.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Specifications JPA pour {@link AuditLog}.
 */
public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> fromCriteria(AuditLogCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getUser() != null && !criteria.getUser().isBlank()) {
                    String pattern = "%" + criteria.getUser().trim().toLowerCase(Locale.ROOT) + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("username")), pattern),
                            cb.like(cb.lower(root.get("userFullName")), pattern)
                    ));
                }
                SpecificationUtils.addEqual(predicates, cb, root, "userId", criteria.getUserId());
                SpecificationUtils.addEnumEqual(predicates, cb, root, "module", criteria.getModule());
                SpecificationUtils.addEnumEqual(predicates, cb, root, "actionType", criteria.getActionType());
                SpecificationUtils.addEnumEqual(predicates, cb, root, "result", criteria.getResult());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "ipAddress", criteria.getIpAddress());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "entityName", criteria.getEntityName());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "entityId", criteria.getEntityId());
                SpecificationUtils.addInstantRange(predicates, cb, root, "actionDate",
                        criteria.getActionDateFrom(), criteria.getActionDateTo());
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
