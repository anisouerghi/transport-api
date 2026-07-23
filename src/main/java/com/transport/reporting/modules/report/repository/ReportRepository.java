package com.transport.reporting.modules.report.repository;

import com.transport.reporting.modules.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReference(String reference);

    boolean existsByReference(String reference);
}
