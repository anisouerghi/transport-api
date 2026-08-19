package com.transport.reporting.repository;

import com.transport.reporting.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository JPA des reponses.
 */
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByReport_ReportIdOrderByReplyDateAsc(Long reportId);

    /** Réponses visibles dans le suivi voyageur. */
    List<Reply> findByReport_ReportIdAndPublicResponseTrueOrderByReplyDateAsc(Long reportId);

    List<Reply> findByReport_ReportIdInAndPublicResponseTrueOrderByReplyDateDesc(List<Long> reportIds);

    boolean existsByReport_ReportId(Long reportId);

    @Query("SELECT DISTINCT r.report.reportId FROM Reply r WHERE r.report.reportId IN :ids")
    List<Long> findReportIdsHavingReplies(@Param("ids") List<Long> ids);
}
