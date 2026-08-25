package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.EmailSendResult;
import com.transport.reporting.dto.ReplyCreateResult;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service métier des réponses agents sur les signalements.
 */
@Service
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
    private final EmailService emailService;
    private final ReplyEmailComposer replyEmailComposer;
    public ReplyService(ReplyRepository replyRepository, ReportRepository reportRepository, ReportHistoryRepository reportHistoryRepository, UserRepository userRepository, StatusRepository statusRepository, ReplyMapper replyMapper, AuditLogService auditLogService, PermissionChecker permissionChecker, EmailService emailService, ReplyEmailComposer replyEmailComposer) {
        this.replyRepository = replyRepository;
        this.reportRepository = reportRepository;
        this.reportHistoryRepository = reportHistoryRepository;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.replyMapper = replyMapper;
        this.auditLogService = auditLogService;
        this.permissionChecker = permissionChecker;
        this.emailService = emailService;
        this.replyEmailComposer = replyEmailComposer;
    }


    @Transactional(readOnly = true)
    public List<ReplyResponse> findByReportId(Long reportId) {
        ensureReportExists(reportId);
        return replyRepository.findByReport_ReportIdOrderByReplyDateAsc(reportId).stream()
                .map(replyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crée une réponse agent et envoie éventuellement un e-mail au voyageur
     * (lien de suivi sécurisé par UUID).
     * <p>
     * La réponse est toujours persistée. Si l'envoi e-mail demandé échoue,
     * le résultat indique {@code success=false} avec un message utilisateur
     * et un {@code errorCode}, sans exception technique.
     */
    public ReplyCreateResult create(Long reportId, ReplyRequest request) {
        Report report = reportRepository.findByIdWithPassenger(reportId)
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
        report.setPublicResponse(publicResponse);
        report.setPublicResponseDate(publicResponse ? Instant.now() : null);

        String passengerEmail = resolvePassengerEmail(report);
        boolean emailRequested = Boolean.TRUE.equals(request.getSendEmail());
        // Sécurité : on n'envoie un e-mail au voyageur que si la réponse est visible pour lui.
        boolean canSendEmail = emailRequested && publicResponse && StringUtils.hasText(passengerEmail);

        Reply reply = Reply.builder()
                .message(request.getMessage().trim())
                .emailSent(false)
                .publicResponse(publicResponse)
                .report(report)
                .appUser(user)
                .build();

        reply = replyRepository.save(reply);

        EmailSendResult emailResult = null;
        if (canSendEmail) {
            try {
                emailResult = sendReplyEmail(report, reply, user, passengerEmail);
            } catch (Exception ex) {
                log.error("Echec technique envoi e-mail pour report {}", reportId, ex);
                emailResult = EmailSendResult.fail("EMAIL_SEND_FAILED",
                        "L'e-mail n'a pas pu être envoyé. Détail : " + ex.getMessage());
            }
        } else if (emailRequested && !publicResponse) {
            // Réponse interne : jamais de lien e-mail, et jamais publiée dans le suivi voyageur.
            emailResult = EmailSendResult.fail(
                    "EMAIL_NOT_PUBLIC",
                    "Réponse enregistrée, mais l'e-mail n'a pas été envoyé car la visibilité voyageur est désactivée."
            );
            log.info("sendEmail requested for report {} but publicResponse=false", reportId);
            auditEmail(user, report, passengerEmail, false, emailResult.getMessage());
        } else if (emailRequested) {
            emailResult = EmailSendResult.fail("EMAIL_NO_RECIPIENT",
                    "La réponse a été enregistrée, mais aucun e-mail voyageur n'est renseigné : aucun envoi effectué.");
            log.warn("sendEmail requested for report {} but passenger has no email", reportId);
            auditEmail(user, report, passengerEmail, false, emailResult.getMessage());
        }

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
                        + ";emailSent=" + reply.isEmailSent()
                        + ";publicResponse=" + publicResponse)
                .description("Réponse agent sur le signalement " + report.getReference())
                .build());

        ReplyResponse response = replyMapper.toResponse(reply);
        response.setEmailRequested(emailRequested);

        if (emailResult == null) {
            response.setEmailMessage("Réponse enregistrée (aucun envoi e-mail demandé).");
            return ReplyCreateResult.builder()
                    .reply(response)
                    .replySaved(true)
                    .success(true)
                    .message("Réponse enregistrée avec succès.")
                    .build();
        }

        response.setEmailMessage(emailResult.getMessage());
        response.setEmailErrorCode(emailResult.getErrorCode());
        response.setEmailSent(emailResult.isSuccess());

        if (emailResult.isSuccess()) {
            return ReplyCreateResult.builder()
                    .reply(response)
                    .replySaved(true)
                    .success(true)
                    .message("Réponse enregistrée. E-mail envoyé à " + passengerEmail
                            + " (accepté par le serveur SMTP — vérifiez aussi les indésirables).")
                    .build();
        }

        return ReplyCreateResult.builder()
                .reply(response)
                .replySaved(true)
                .success(false)
                .message("Réponse enregistrée, mais l'e-mail n'a pas pu être envoyé. " + emailResult.getMessage())
                .errorCode(emailResult.getErrorCode())
                .build();
    }

    private EmailSendResult sendReplyEmail(Report report, Reply reply, AppUser user, String passengerEmail) {
        String html = replyEmailComposer.buildHtml(report, reply);
        EmailSendResult result = emailService.sendHtml(passengerEmail, replyEmailComposer.subject(), html);
        reply.setEmailSent(result.isSuccess());
        replyRepository.save(reply);
        if (result.isSuccess()) {
            report.setSendEmail(true);
            report.setSendEmailDate(Instant.now());
        }
        auditEmail(user, report, passengerEmail, result.isSuccess(),
                result.isSuccess() ? null : result.getMessage());
        return result;
    }

    private void auditEmail(AppUser user, Report report, String recipient, boolean success, String errorMessage) {
        auditLogService.record(AuditLogEvent.builder()
                .userId(user != null ? user.getUserId() : null)
                .username(user != null ? user.getUsername() : null)
                .userFullName(user != null ? user.getName() : null)
                .actionType(AuditAction.EMAIL_SEND)
                .module(AuditModule.REPLIES)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .result(success ? AuditResult.SUCCESS : AuditResult.FAILURE)
                .newValue("to=" + (recipient != null ? recipient : "")
                        + ";sentAt=" + Instant.now()
                        + (errorMessage != null ? ";error=" + truncate(errorMessage, 400) : ""))
                .description(success
                        ? "E-mail de réponse envoyé pour " + report.getReference()
                        : "Échec envoi e-mail pour " + report.getReference())
                .build());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
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
