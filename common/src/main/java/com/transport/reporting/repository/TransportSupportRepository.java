package com.transport.reporting.repository;

import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.entity.TransportSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA des supports de transport.
 */
public interface TransportSupportRepository extends JpaRepository<TransportSupport, Long>,
        JpaSpecificationExecutor<TransportSupport> {

    Optional<TransportSupport> findByUuid(UUID uuid);

    @Query("SELECT s FROM TransportSupport s JOIN FETCH s.supportType WHERE s.supportStatus = :status ORDER BY s.supportType.label ASC, s.reference ASC")
    List<TransportSupport> findBySupportStatusWithType(SupportStatus status);

    boolean existsByReference(String reference);

    boolean existsByReferenceAndTransportSupportIdNot(String reference, Long transportSupportId);

    /**
     * Nombre de supports par type de support, du plus fréquent au moins fréquent.
     * Chaque ligne est un couple {@code [SupportType, Long count]}.
     */
    @Query("""
            SELECT s.supportType, COUNT(s)
            FROM TransportSupport s
            WHERE s.supportType IS NOT NULL
            GROUP BY s.supportType
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countSupportsGroupedBySupportType();
}
