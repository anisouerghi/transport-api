package com.transport.reporting.repository;

import com.transport.reporting.entity.ReportSatisfaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportSatisfactionRepository extends JpaRepository<ReportSatisfaction, Long> {

    Optional<ReportSatisfaction> findByReport_ReportId(Long reportId);

    boolean existsByReport_ReportId(Long reportId);

    @Query("""
            SELECT s.level, COUNT(s)
            FROM ReportSatisfaction s
            JOIN s.report r
            WHERE r.status.code IN ('RESOLVED', 'CLOSED')
              AND (:from IS NULL OR r.closureDate >= :from)
              AND (:to IS NULL OR r.closureDate < :to)
              AND (:typeId IS NULL OR r.reportType.reportTypeId = :typeId)
              AND (:supportId IS NULL OR r.transportSupport.transportSupportId = :supportId)
            GROUP BY s.level
            """)
    List<Object[]> countByLevel(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("typeId") Long typeId,
            @Param("supportId") Long supportId);

    @Query("""
            SELECT s FROM ReportSatisfaction s
            JOIN s.report r
            WHERE s.comment IS NOT NULL
              AND r.status.code IN ('RESOLVED', 'CLOSED')
              AND (:from IS NULL OR r.closureDate >= :from)
              AND (:to IS NULL OR r.closureDate < :to)
              AND (:typeId IS NULL OR r.reportType.reportTypeId = :typeId)
              AND (:supportId IS NULL OR r.transportSupport.transportSupportId = :supportId)
            """)
    Page<ReportSatisfaction> findComments(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("typeId") Long typeId,
            @Param("supportId") Long supportId,
            Pageable pageable);
}
