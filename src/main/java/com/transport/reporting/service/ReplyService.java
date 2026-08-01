package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReplyRequest;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportHistory;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReplyMapper;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.security.PermissionChecker;
import com.transport.reporting.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service métier des réponses agents sur les signalements.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReplyService {

    private static final Set<String> CLOSED_STATUS_CODES = Set.of("RESOLVED", "CLOSED");

    private final ReplyRepository replyRepository;
    private final ReportRepository reportRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final ReplyMapper replyMapper;
    private final AuditLogService auditLogService;
    private final PermissionChecker permissionChecker;

    @Transactional(readOnly = true)
    public List<ReplyResponse> findByReportId(Long reportId) {
        ensureReportExists(reportId);
        return replyRepository.findByReport_ReportIdOrderByReplyDateAsc(reportId).stream()
                .map(replyMapper::toResponse)
                .toList();
    }

    /**
     * Crée une réponse agent.
     * <ul>
     *   <li>{@code publicResponse} : visibilité suivi voyageur (défaut true)</li>
     *   <li>{@code sendEmail} : notification e-mail si le voyageur a une adresse</li>
     * </ul>
     */
    public ReplyResponse create(Long reportId, ReplyRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        AppUser user = resolveActor(request.getUserId());

        if (request.getStatusId() != null
                && (report.getStatus() == null || !Objects.equals(report.getStatus().getStatusId(), request.getStatusId()))) {
            if (!permissionChecker.has("REPORT", "CLOSE") && !permissionChecker.has("REPORT", "EDIT")) {
                throw new BusinessException("Permission REPORT_CLOSE or REPORT_EDIT required to change status");
            }
            applyStatusChange(report, request.getStatusId(), user, request.getMessage());
        }

        if (request.getPublish() != null) {
            report.setPublish(request.getPublish());
            report.setPublishDate(Boolean.TRUE.equals(request.getPublish()) ? Instant.now() : null);
        }

        boolean publicResponse = request.getPublicResponse() == null || Boolean.TRUE.equals(request.getPublicResponse());
        // Miroir legacy sur le signalement (compatibilité écrans existants)
        report.setPublicResponse(publicResponse);
        report.setPublicResponseDate(publicResponse ? Instant.now() : null);

        String passengerEmail = resolvePassengerEmail(report);
        boolean emailRequested = Boolean.TRUE.equals(request.getSendEmail());
        boolean emailSent = emailRequested && StringUtils.hasText(passengerEmail);
        if (emailRequested && !StringUtils.hasText(passengerEmail)) {
            log.warn("sendEmail requested for report {} but passenger has no email", reportId);
        }
        if (emailSent) {
            report.setSendEmail(true);
            report.setSendEmailDate(Instant.now());
            log.info("Email notification requested for report {} to {} (sending not implemented yet)",
                    reportId, passengerEmail);
        }

        Reply reply = Reply.builder()
                .message(request.getMessage().trim())
                .emailSent(emailSent)
                .publicResponse(publicResponse)
                .report(report)
                .appUser(user)
                .build();

        reply = replyRepository.save(reply);
        reportRepository.save(report);

        auditLogService.record(AuditLogEvent.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .userFullName(user.getName())
                .actionType(AuditAction.REPLY)
                .module(AuditModule.REPLIES)
                .entityName("Reply")
                .entityId(String.valueOf(reply.getReplyId()))
                .newValue("reportId=" + reportId
                        + ";emailSent=" + emailSent
                        + ";publicResponse=" + publicResponse)
                .description("Réponse agent sur le signalement " + report.getReference())
                .build());

        return replyMapper.toResponse(reply);
    }

    private static String resolvePassengerEmail(Report report) {
        Passenger passenger = report.getPassenger();
        if (passenger == null) {
            return null;
        }
        return StringUtils.hasText(passenger.getEmail()) ? passenger.getEmail().trim() : null;
    }

    private void applyStatusChange(Report report, Long newStatusId, AppUser user, String comments) {
        Status newStatus = statusRepository.findById(newStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", newStatusId));

        Status oldStatus = report.getStatus();
        String oldCode = oldStatus != null ? oldStatus.getCode() : null;
        report.setStatus(newStatus);

        if (CLOSED_STATUS_CODES.contains(newStatus.getCode()) && report.getClosureDate() == null) {
            report.setClosureDate(Instant.now());
        } else if (!CLOSED_STATUS_CODES.contains(newStatus.getCode())) {
            report.setClosureDate(null);
        }

        reportRepository.save(report);
        reportHistoryRepository.save(ReportHistory.builder()
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .comments(comments != null && comments.length() > 1000 ? comments.substring(0, 1000) : comments)
                .report(report)
                .appUser(user)
                .build());

        auditLogService.record(AuditLogEvent.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .userFullName(user.getName())
                .actionType(AuditAction.STATUS_CHANGE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .oldValue("status=" + oldCode)
                .newValue("status=" + newStatus.getCode())
                .description("Changement de statut du signalement " + report.getReference())
                .build());
    }

    private AppUser resolveActor(Long requestedUserId) {
        Long userId = requestedUserId != null ? requestedUserId : SecurityUtils.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("Authenticated user is required to reply");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void ensureReportExists(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report", reportId);
        }
    }
}
