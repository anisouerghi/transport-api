package com.transport.reporting.service;

import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Suivi public sécurisé des signalements (accès par UUID uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicTrackingService {

    private final ReportRepository reportRepository;
    private final ReplyRepository replyRepository;

    /**
     * Charge le détail public d'un signalement et les réponses visibles au voyageur.
     */
    public PublicReportTrackingResponse findByUuid(UUID uuid) {
        Report report = reportRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Report", uuid));

        List<PublicReportTrackingResponse.PublicReplyView> replies =
                replyRepository.findByReport_ReportIdAndPublicResponseTrueOrderByReplyDateAsc(report.getReportId())
                        .stream()
                        .map(this::toPublicReply)
                        .toList();

        String supportLabel = null;
        if (report.getTransportSupport() != null) {
            supportLabel = report.getTransportSupport().getLabel() != null
                    ? report.getTransportSupport().getLabel()
                    : report.getTransportSupport().getReference();
        }

        return PublicReportTrackingResponse.builder()
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .description(report.getDescription())
                .reportTypeLabel(report.getReportType() != null ? report.getReportType().getLabel() : null)
                .supportLabel(supportLabel)
                .statusCode(report.getStatus() != null ? report.getStatus().getCode() : null)
                .statusLabel(report.getStatus() != null ? report.getStatus().getLabel() : null)
                .replies(replies)
                .build();
    }

    private PublicReportTrackingResponse.PublicReplyView toPublicReply(Reply reply) {
        return PublicReportTrackingResponse.PublicReplyView.builder()
                .message(reply.getMessage())
                .replyDate(reply.getReplyDate())
                .build();
    }
}
