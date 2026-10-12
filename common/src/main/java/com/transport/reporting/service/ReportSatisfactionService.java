package com.transport.reporting.service;

import com.transport.reporting.common.enums.SatisfactionLevel;
import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.PublicSatisfactionRequest;
import com.transport.reporting.dto.PublicSatisfactionResponse;
import com.transport.reporting.dto.SatisfactionCommentResponse;
import com.transport.reporting.dto.SatisfactionSummaryResponse;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportSatisfaction;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportSatisfactionRepository;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Avis unique du voyageur sur un signalement clôturé.
 * Un compte avec suivi n'est accepté que si le jeton est celui du propriétaire.
 * Un dossier sans compte reste accessible par le lien de suivi UUID, comme la consultation.
 */
@Service
public class ReportSatisfactionService {

    static final int COMMENT_MAX = 1000;

    private final ReportRepository reportRepository;
    private final ReportSatisfactionRepository satisfactionRepository;

    public ReportSatisfactionService(
            ReportRepository reportRepository,
            ReportSatisfactionRepository satisfactionRepository) {
        this.reportRepository = reportRepository;
        this.satisfactionRepository = satisfactionRepository;
    }

    @Transactional
    public PublicSatisfactionResponse submit(java.util.UUID reportUuid, PublicSatisfactionRequest request) {
        Report report = reportRepository.findByUuid(reportUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportUuid));
        assertCanSubmit(report);
        if (satisfactionRepository.existsByReport_ReportId(report.getReportId())) {
            throw alreadyExists();
        }
        ReportSatisfaction saved;
        try {
            saved = satisfactionRepository.save(ReportSatisfaction.builder()
                    .report(report)
                    .level(request.getLevel())
                    .comment(sanitizeComment(request.getComment()))
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw alreadyExists();
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PublicSatisfactionResponse findForReport(Long reportId) {
        return satisfactionRepository.findByReport_ReportId(reportId)
                .map(ReportSatisfactionService::toResponse)
                .orElse(null);
    }

    /**
     * Vrai si ce visiteur peut encore déposer l'avis unique.
     * {@code passengerId} est nul lorsque la requête de suivi n'a pas de jeton.
     */
    public boolean canSubmit(Report report, Long passengerId) {
        if (report == null || report.getReportId() == null || !ReplyService.isClosed(report)) {
            return false;
        }
        if (satisfactionRepository.existsByReport_ReportId(report.getReportId())) {
            return false;
        }
        return isAllowed(report, passengerId);
    }

    /** Le dossier suivi exige une connexion du propriétaire avant l'avis. */
    public boolean loginRequired(Report report, Long passengerId) {
        if (report == null || !ReplyService.isClosed(report) || !isTracked(report)) {
            return false;
        }
        if (report.getReportId() != null && satisfactionRepository.existsByReport_ReportId(report.getReportId())) {
            return false;
        }
        return !isOwner(report, passengerId);
    }

    @Transactional(readOnly = true)
    public SatisfactionSummaryResponse summarize(LocalDate from, LocalDate to, Long reportTypeId, Long supportId) {
        Instant start = startOf(from);
        Instant end = startOfNextDay(to);
        long eligible = reportRepository.countClosedEligible(start, end, reportTypeId, supportId);
        Map<SatisfactionLevel, Long> counts = new EnumMap<>(SatisfactionLevel.class);
        for (Object[] row : satisfactionRepository.countByLevel(start, end, reportTypeId, supportId)) {
            counts.put((SatisfactionLevel) row[0], (Long) row[1]);
        }
        long satisfied = counts.getOrDefault(SatisfactionLevel.SATISFAIT, 0L);
        long partial = counts.getOrDefault(SatisfactionLevel.MOYENNEMENT_SATISFAIT, 0L);
        long dissatisfied = counts.getOrDefault(SatisfactionLevel.NON_SATISFAIT, 0L);
        long received = satisfied + partial + dissatisfied;
        return SatisfactionSummaryResponse.builder()
                .eligibleClosedReports(eligible)
                .evaluationsReceived(received)
                .participationRate(percent(received, eligible))
                .satisfiedCount(satisfied)
                .satisfiedRate(percent(satisfied, received))
                .partialCount(partial)
                .partialRate(percent(partial, received))
                .dissatisfiedCount(dissatisfied)
                .dissatisfiedRate(percent(dissatisfied, received))
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<SatisfactionCommentResponse> comments(
            LocalDate from, LocalDate to, Long reportTypeId, Long supportId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        var result = satisfactionRepository.findComments(
                startOf(from),
                startOfNextDay(to),
                reportTypeId,
                supportId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<SatisfactionCommentResponse> content = result.getContent().stream()
                .map(ReportSatisfactionService::toComment)
                .toList();
        return PageResponse.<SatisfactionCommentResponse>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    /**
     * Retire les balises et les caractères nuls. Vide devient nul.
     * La longueur est déjà bornée par la validation du DTO.
     */
    static String sanitizeComment(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace("\u0000", "");
        String previous;
        do {
            previous = cleaned;
            cleaned = cleaned.replaceAll("<[^>]*>", "");
        } while (!cleaned.equals(previous));
        cleaned = cleaned.replace("<", "").replace(">", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.length() > COMMENT_MAX) {
            throw new BusinessException("Le commentaire dépasse 1000 caractères.", "SATISFACTION_COMMENT_INVALID");
        }
        return cleaned;
    }

    private void assertCanSubmit(Report report) {
        if (!ReplyService.isClosed(report)) {
            throw new BusinessException(
                    "Le signalement n'est pas clôturé.",
                    "SATISFACTION_NOT_CLOSED");
        }
        PassengerPrincipal principal = SecurityUtils.currentPassenger().orElse(null);
        Long passengerId = principal == null ? null : principal.getPassengerId();
        if (isTracked(report) && passengerId == null) {
            throw new BusinessException("Authentification requise.", "AUTH_REQUIRED");
        }
        if (!isAllowed(report, passengerId)) {
            throw new BusinessException(
                    "Vous n'êtes pas autorisé à évaluer ce signalement.",
                    "SATISFACTION_FORBIDDEN");
        }
    }

    private static boolean isAllowed(Report report, Long passengerId) {
        if (!isTracked(report)) {
            return true;
        }
        return isOwner(report, passengerId);
    }

    private static boolean isOwner(Report report, Long passengerId) {
        Passenger passenger = report.getPassenger();
        return passengerId != null
                && passenger != null
                && passengerId.equals(passenger.getPassengerId());
    }

    private static boolean isTracked(Report report) {
        Passenger passenger = report.getPassenger();
        return passenger != null && passenger.hasTrackedAccount();
    }

    private static BusinessException alreadyExists() {
        return new BusinessException(
                "Un avis a déjà été enregistré pour ce signalement.",
                "SATISFACTION_ALREADY_EXISTS");
    }

    private static PublicSatisfactionResponse toResponse(ReportSatisfaction satisfaction) {
        return PublicSatisfactionResponse.builder()
                .level(satisfaction.getLevel().name())
                .comment(satisfaction.getComment())
                .createdAt(satisfaction.getCreatedAt())
                .build();
    }

    private static SatisfactionCommentResponse toComment(ReportSatisfaction satisfaction) {
        Report report = satisfaction.getReport();
        String supportLabel = null;
        if (report.getTransportSupport() != null) {
            supportLabel = report.getTransportSupport().getLabel() != null
                    ? report.getTransportSupport().getLabel()
                    : report.getTransportSupport().getReference();
        }
        return SatisfactionCommentResponse.builder()
                .reference(report.getReference())
                .reportTypeLabel(report.getReportType() != null ? LocalizedLabels.of(report.getReportType()) : null)
                .supportLabel(supportLabel)
                .level(satisfaction.getLevel().name())
                .comment(satisfaction.getComment())
                .createdAt(satisfaction.getCreatedAt())
                .build();
    }

    private static double percent(long part, long total) {
        if (total <= 0) {
            return 0d;
        }
        return Math.round(part * 1000d / total) / 10d;
    }

    private static Instant startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfNextDay(LocalDate day) {
        return day == null ? null : day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
