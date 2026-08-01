package com.transport.reporting.specification;

import com.transport.reporting.common.util.SpecificationUtils;
import com.transport.reporting.dto.PassengerCriteria;
import com.transport.reporting.entity.Passenger;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PassengerSpecification {

    private PassengerSpecification() {
    }

    public static Specification<Passenger> fromCriteria(PassengerCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "name", criteria.getName());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "email", criteria.getEmail());
                SpecificationUtils.addLikeIgnoreCase(predicates, cb, root, "phoneNumber", criteria.getPhoneNumber());
                if (criteria.getActive() != null) {
                    predicates.add(cb.equal(root.get("active"), criteria.getActive()));
                }
            }
            return SpecificationUtils.andAll(predicates, cb);
        };
    }
}
