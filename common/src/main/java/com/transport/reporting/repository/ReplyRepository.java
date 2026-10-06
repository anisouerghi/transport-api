package com.transport.reporting.repository;

import com.transport.reporting.common.enums.ReplyType;
import com.transport.reporting.entity.Reply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository JPA des reponses.
 */
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    /**
     * Conversation complète d'un signalement, du plus ancien au plus récent.
     * {@code replyId} départage deux messages enregistrés à la même seconde.
     */
    List<Reply> findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(Long reportId);

    /**
     * Messages publics d'un signalement, hors type exclu (note interne),
     * du plus ancien au plus récent.
     */
    List<Reply> findByReport_ReportIdAndPublicResponseTrueAndReplyTypeNotOrderByReplyDateAscReplyIdAsc(
            Long reportId, ReplyType replyType);

    List<Reply> findByReport_ReportIdInAndPublicResponseTrueAndReplyTypeNotOrderByReplyDateDesc(
            List<Long> reportIds, ReplyType replyType);

    boolean existsByReport_ReportId(Long reportId);

    @Query("SELECT DISTINCT r.report.reportId FROM Reply r WHERE r.report.reportId IN :ids")
    List<Long> findReportIdsHavingReplies(@Param("ids") List<Long> ids);

    /**
     * Accueil public : réponses publiées, visibles du voyageur, hors notes internes.
     * L'appelant borne le nombre de lignes (12).
     */
    @Query("""
            SELECT r FROM Reply r
            WHERE r.report.publish = true
              AND r.publicResponse = true
              AND (r.replyType IS NULL OR r.replyType <> :excluded)
            ORDER BY r.replyDate DESC, r.replyId DESC
            """)
    List<Reply> findPublicHomepageReplies(@Param("excluded") ReplyType excluded, Pageable pageable);
}
