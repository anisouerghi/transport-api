package com.transport.reporting.repository;

import com.transport.reporting.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReference(String reference);

    boolean existsByReference(String reference);
}
