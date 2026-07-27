package com.transport.reporting.repository;

import com.transport.reporting.entity.TransportSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA des supports de transport.
 */
public interface TransportSupportRepository extends JpaRepository<TransportSupport, Long>,
        JpaSpecificationExecutor<TransportSupport> {

    Optional<TransportSupport> findByUuid(UUID uuid);

    boolean existsByReference(String reference);

    boolean existsByReferenceAndTransportSupportIdNot(String reference, Long transportSupportId);
}
