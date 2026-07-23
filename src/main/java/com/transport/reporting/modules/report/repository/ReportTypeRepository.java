package com.transport.reporting.modules.report.repository;

import com.transport.reporting.modules.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportTypeRepository extends JpaRepository<ReportType, Long> {

    Optional<ReportType> findByCode(String code);
}
