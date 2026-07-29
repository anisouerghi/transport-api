package com.transport.reporting.repository;

import com.transport.reporting.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repository JPA des signalements.
 */
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Optional<Report> findByReference(String reference);

    boolean existsByReference(String reference);
}
