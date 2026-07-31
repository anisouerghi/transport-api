package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReplyRequest;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportHistory;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReplyMapper;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service metier des reponses agents sur les signalements.
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

    @Transactional(readOnly = true)
    public List<ReplyResponse> findByReportId(Long reportId) {
        ensureReportExists(reportId);
        return replyRepository.findByReport_ReportIdOrderByReplyDateAsc(reportId).stream()
                .map(replyMapper::toResponse)
                .toList();
    }

    public ReplyResponse create(Long reportId, ReplyRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        if (request.getStatusId() != null
                && (report.getStatus() == null || !Objects.equals(report.getStatus().getStatusId(), request.getStatusId()))) {
            applyStatusChange(report, request.getStatusId(), user, request.getMessage());
        }

        if (request.getPublish() != null) {
            report.setPublish(request.getPublish());
            if (Boolean.TRUE.equals(request.getPublish())) {
                if (report.getPublishDate() == null) {
                    report.setPublishDate(Instant.now());
                }
            } else {
                report.setPublishDate(null);
            }
        }

        if (request.getPublicResponse() != null) {
            report.setPublicResponse(request.getPublicResponse());
            if (Boolean.TRUE.equals(request.getPublicResponse())) {
                if (report.getPublicResponseDate() == null) {
                    report.setPublicResponseDate(Instant.now());
                }
            } else {
                report.setPublicResponseDate(null);
            }
        }

        if (request.getSendEmail() != null) {
            report.setSendEmail(request.getSendEmail());
            if (Boolean.TRUE.equals(request.getSendEmail())) {
                if (report.getSendEmailDate() == null) {
                    report.setSendEmailDate(Instant.now());
                }
            } else {
                report.setSendEmailDate(null);
            }
        }

        boolean emailSent = Boolean.TRUE.equals(request.getSendEmail());
        if (emailSent) {
            log.info("Email notification requested for report {} (sending not implemented yet)", reportId);
        }

        Reply reply = Reply.builder()
                .message(request.getMessage().trim())
                .emailSent(emailSent)
                .report(report)
                .appUser(user)
                .build();

        reply = replyRepository.save(reply);

        auditLogService.record(AuditLogEvent.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .userFullName(user.getName())
                .actionType(AuditAction.REPLY)
                .module(AuditModule.REPLIES)
                .entityName("Reply")
                .entityId(String.valueOf(reply.getReplyId()))
                .newValue("reportId=" + reportId + ";emailSent=" + emailSent)
                .description("Réponse agent sur le signalement " + report.getReference())
                .build());

        return replyMapper.toResponse(reply);
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

    private void ensureReportExists(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report", reportId);
        }
    }
}
