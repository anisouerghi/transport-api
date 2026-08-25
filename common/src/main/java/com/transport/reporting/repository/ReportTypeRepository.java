package com.transport.reporting.repository;

import com.transport.reporting.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repository JPA des types de signalement.
 */
public interface ReportTypeRepository extends JpaRepository<ReportType, Long>, JpaSpecificationExecutor<ReportType> {

    Optional<ReportType> findByCode(String code);

    boolean existsByCode(String code);
}
