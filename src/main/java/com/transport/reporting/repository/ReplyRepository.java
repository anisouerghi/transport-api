package com.transport.reporting.repository;

import com.transport.reporting.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository JPA des reponses.
 */
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByReport_ReportIdOrderByReplyDateAsc(Long reportId);

    /** Réponses visibles dans le suivi voyageur. */
    List<Reply> findByReport_ReportIdAndPublicResponseTrueOrderByReplyDateAsc(Long reportId);
}
