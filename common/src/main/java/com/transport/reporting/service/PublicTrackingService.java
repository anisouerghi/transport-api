package com.transport.reporting.service;

import com.transport.reporting.common.i18n.LocalizedLabels;
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

    private static final int HOMEPAGE_REPLY_LIMIT = 12;
    private static final int HOMEPAGE_PAGE_SIZE = 4;
    /** Index 0-based : pages autorisées = 0, 1, 2. */
    private static final int HOMEPAGE_MAX_PAGE_INDEX = 2;

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
                .reportTypeLabel(report.getReportType() != null ? LocalizedLabels.of(report.getReportType()) : null)
                .supportLabel(supportLabel)
                .statusCode(report.getStatus() != null ? report.getStatus().getCode() : null)
                .statusLabel(report.getStatus() != null ? LocalizedLabels.of(report.getStatus()) : null)
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
     * Accueil public : au plus 12 réponses des signalements {@code publish},
     * paginées par 4 (pages 0..2), plus récentes d'abord.
     * {@code size} et {@code page} sont bornés côté serveur (non contournables).
     */
    public PageResponse<PublicHomepageReplyResponse> listHomepageReplies(int page, int size) {
        int safeSize = size <= 0 ? HOMEPAGE_PAGE_SIZE : Math.min(size, HOMEPAGE_PAGE_SIZE);
        List<Reply> latest = replyRepository.findTop12ByReport_PublishTrueOrderByReplyDateDesc()
                .stream()
                .sorted(Comparator.comparing(
                        Reply::getReplyDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(HOMEPAGE_REPLY_LIMIT)
                .collect(Collectors.toList());
        int total = latest.size();
        int totalPages = total == 0
                ? 0
                : Math.min((int) Math.ceil(total / (double) safeSize), HOMEPAGE_MAX_PAGE_INDEX + 1);

        // page hors plage autorisée (ex. page=3) → contenu vide, pas de fuite via clamp
        if (page < 0 || page > HOMEPAGE_MAX_PAGE_INDEX) {
            return PageResponse.<PublicHomepageReplyResponse>builder()
                    .content(List.of())
                    .totalElements(total)
                    .totalPages(totalPages)
                    .page(page < 0 ? 0 : page)
                    .size(safeSize)
                    .build();
        }

        int safePage = page;
        if (totalPages > 0 && safePage >= totalPages) {
            // page dans 0..2 mais au-delà des données disponibles → vide
            return PageResponse.<PublicHomepageReplyResponse>builder()
                    .content(List.of())
                    .totalElements(total)
                    .totalPages(totalPages)
                    .page(safePage)
                    .size(safeSize)
                    .build();
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
        String passengerName = null;
        if (report != null && report.getReportType() != null) {
            typeLabel = LocalizedLabels.of(report.getReportType());
        }
        if (report != null && report.getPassenger() != null) {
            passengerName = report.getPassenger().getName();
        }
        return PublicHomepageReplyResponse.builder()
            .description(report != null ? report.getDescription() : null)
                .message(reply.getMessage())
                .passengerName(passengerName)
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
                supportTypeLabel = LocalizedLabels.of(report.getTransportSupport().getSupportType());
            }
        }
        return PublicReportListItemResponse.builder()
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .supportLabel(supportLabel)
                .supportTypeLabel(supportTypeLabel)
                .statusCode(report.getStatus() != null ? report.getStatus().getCode() : null)
                .statusLabel(report.getStatus() != null ? LocalizedLabels.of(report.getStatus()) : null)
                .build();
    }

    private PublicReportTrackingResponse.PublicReplyView toPublicReply(Reply reply) {
        return PublicReportTrackingResponse.PublicReplyView.builder()
                .message(reply.getMessage())
                .replyDate(reply.getReplyDate())
                .build();
    }
}
