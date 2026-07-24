package com.transport.reporting.repository;

import com.transport.reporting.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository JPA de l'historique des signalements.
 */
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

    List<ReportHistory> findByReport_ReportIdOrderByActionDateAsc(Long reportId);
}
