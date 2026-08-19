package com.transport.reporting.service;

import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.PublicHomepageReplyResponse;
import com.transport.reporting.dto.PublicReportListItemResponse;
import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Suivi public sécurisé des signalements (accès par UUID uniquement).
 */
@Service
@Transactional(readOnly = true)
public class PublicTrackingService {

    private static final int HOMEPAGE_REPLY_LIMIT = 15;
    private static final int HOMEPAGE_PAGE_SIZE = 5;

    private final ReportRepository reportRepository;
    private final ReplyRepository replyRepository;
    public PublicTrackingService(ReportRepository reportRepository, ReplyRepository replyRepository) {
        this.reportRepository = reportRepository;
        this.replyRepository = replyRepository;
    }


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
                        .collect(Collectors.toList());

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

    /**
     * 15 derniers signalements du voyageur authentifié (identité = JWT uniquement).
     * Filtre optionnel par référence (partiel, insensible à la casse).
     */
    public List<PublicReportListItemResponse> listMine(Long passengerId, String reference) {
        List<Report> reports = StringUtils.hasText(reference)
                ? reportRepository.findTop15ByPassenger_PassengerIdAndReferenceContainingIgnoreCaseOrderByCreationDateDesc(
                        passengerId, reference.trim())
                : reportRepository.findTop15ByPassenger_PassengerIdOrderByCreationDateDesc(passengerId);

        return reports.stream()
                .sorted(Comparator.comparing(Report::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(15)
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Accueil public : 15 dernières réponses des signalements {@code publish} (case « Visible à l'accueil »),
     * paginées par 5, plus récentes d'abord. Aucune donnée personnelle.
     */
    public PageResponse<PublicHomepageReplyResponse> listHomepageReplies(int page, int size) {
        int safeSize = size <= 0 ? HOMEPAGE_PAGE_SIZE : Math.min(size, HOMEPAGE_PAGE_SIZE);
        List<Reply> latest = replyRepository.findTop15ByReport_PublishTrueOrderByReplyDateDesc();
        int total = Math.min(latest.size(), HOMEPAGE_REPLY_LIMIT);
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) safeSize);
        int safePage = Math.max(page, 0);
        if (totalPages > 0 && safePage >= totalPages) {
            safePage = totalPages - 1;
        }
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<PublicHomepageReplyResponse> content = latest.subList(from, to).stream()
                .map(this::toHomepageReply)
                .collect(Collectors.toList());
        return PageResponse.<PublicHomepageReplyResponse>builder()
                .content(content)
                .totalElements(total)
                .totalPages(totalPages)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    private PublicHomepageReplyResponse toHomepageReply(Reply reply) {
        Report report = reply.getReport();
        String typeLabel = null;
        if (report != null && report.getReportType() != null) {
            typeLabel = report.getReportType().getLabel();
        }
        return PublicHomepageReplyResponse.builder()
                .message(reply.getMessage())
                .replyDate(reply.getReplyDate())
                .reportTypeLabel(typeLabel)
                .build();
    }

    private PublicReportListItemResponse toListItem(Report report) {
        String supportLabel = null;
        String supportTypeLabel = null;
        if (report.getTransportSupport() != null) {
            supportLabel = report.getTransportSupport().getLabel() != null
                    ? report.getTransportSupport().getLabel()
                    : report.getTransportSupport().getReference();
            if (report.getTransportSupport().getSupportType() != null) {
                supportTypeLabel = report.getTransportSupport().getSupportType().getLabel();
            }
        }
        return PublicReportListItemResponse.builder()
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .supportLabel(supportLabel)
                .supportTypeLabel(supportTypeLabel)
                .statusCode(report.getStatus() != null ? report.getStatus().getCode() : null)
                .statusLabel(report.getStatus() != null ? report.getStatus().getLabel() : null)
                .build();
    }

    private PublicReportTrackingResponse.PublicReplyView toPublicReply(Reply reply) {
        return PublicReportTrackingResponse.PublicReplyView.builder()
                .message(reply.getMessage())
                .replyDate(reply.getReplyDate())
                .build();
    }
}
