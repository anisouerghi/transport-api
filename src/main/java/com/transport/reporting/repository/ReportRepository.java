package com.transport.reporting.repository;

import com.transport.reporting.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA des signalements.
 */
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Optional<Report> findByReference(String reference);

    Optional<Report> findByUuid(UUID uuid);

    List<Report> findTop15ByPassenger_PassengerIdOrderByCreationDateDesc(Long passengerId);

    List<Report> findTop15ByPassenger_PassengerIdAndReferenceContainingIgnoreCaseOrderByCreationDateDesc(
            Long passengerId, String reference);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.passenger LEFT JOIN FETCH r.status WHERE r.reportId = :id")
    Optional<Report> findByIdWithPassenger(@Param("id") Long id);

    boolean existsByReference(String reference);

    boolean existsByTransportSupportTransportSupportId(Long transportSupportId);
}
