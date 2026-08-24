package com.transport.reporting.repository;

import com.transport.reporting.entity.ReportNature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ReportNatureRepository extends JpaRepository<ReportNature, Long>, JpaSpecificationExecutor<ReportNature> {

    Optional<ReportNature> findByCode(String code);

    boolean existsByCode(String code);

    List<ReportNature> findByActiveTrueOrderByLabelAsc();
}
