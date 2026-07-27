package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.TransportSupportCriteria;
import com.transport.reporting.entity.TransportSupport;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications JPA pour TransportSupport.
 * Convertit {@link TransportSupportCriteria} en predicats Criteria API.
 */
public final class TransportSupportSpecification {

    private TransportSupportSpecification() {
    }

    /**
     * Construit une Specification a partir des criteres (tous optionnels).
     * UUID invalide = aucun resultat (disjunction = false).
     */
    public static Specification<TransportSupport> fromCriteria(TransportSupportCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "reference", criteria.getReference());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "label", criteria.getLabel());

                // UUID : egalite exacte si la chaine est un UUID valide
                if (criteria.getUuid() != null && !criteria.getUuid().isBlank()) {
                    try {
                        UUID uuid = UUID.fromString(criteria.getUuid().trim());
                        predicates.add(cb.equal(root.get("uuid"), uuid));
                    } catch (IllegalArgumentException ignored) {
                        // UUID mal forme -> aucun resultat
                        predicates.add(cb.disjunction());
                    }
                }

                SpecificationUtils.addEnumEqual(predicates, cb, root, "qrStatus", criteria.getQrStatus());
                SpecificationUtils.addEnumEqual(predicates, cb, root, "supportStatus", criteria.getSupportStatus());

                // Filtre sur la relation ManyToOne supportType
                if (criteria.getSupportTypeId() != null) {
                    predicates.add(cb.equal(root.join("supportType").get("supportTypeId"), criteria.getSupportTypeId()));
                }

                SpecificationUtils.addInstantRange(predicates, cb, root, "qrDateCreation",
                        criteria.getQrDateCreationFrom(), criteria.getQrDateCreationTo());
                SpecificationUtils.addInstantRange(predicates, cb, root, "qrDateImpression",
                        criteria.getQrDateImpressionFrom(), criteria.getQrDateImpressionTo());
                SpecificationUtils.addInstantRange(predicates, cb, root, "createdAt",
                        criteria.getCreatedAtFrom(), criteria.getCreatedAtTo());
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
