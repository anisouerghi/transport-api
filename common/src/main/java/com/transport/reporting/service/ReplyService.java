package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import com.transport.reporting.common.enums.ReplyAuthorType;
import com.transport.reporting.common.enums.ReplyType;
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
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.security.PermissionChecker;
import com.transport.reporting.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Conversation d'un signalement : réponses agents, demandes de complément
 * et réponses du voyageur. L'historique des statuts reste dans {@link ReportHistory}.
 */
@Service
@Transactional
@Slf4j
public class ReplyService {

    private static final Set<String> CLOSED_STATUS_CODES = Set.of("RESOLVED", "CLOSED");
    private static final String STATUS_COMPLEMENT = "DEMANDE_COMPLEMENT";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final ReplyRepository replyRepository;
    private final ReportRepository reportRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final StatusRepository statusRepository;
    private final ReplyMapper replyMapper;
    private final AuditLogService auditLogService;
    private final PermissionChecker permissionChecker;
    private final EmailService emailService;
    private final ReplyEmailComposer replyEmailComposer;
    public ReplyService(ReplyRepository replyRepository, ReportRepository reportRepository, ReportHistoryRepository reportHistoryRepository, UserRepository userRepository, PassengerRepository passengerRepository, StatusRepository statusRepository, ReplyMapper replyMapper, AuditLogService auditLogService, PermissionChecker permissionChecker, EmailService emailService, ReplyEmailComposer replyEmailComposer) {
        this.replyRepository = replyRepository;
        this.reportRepository = reportRepository;
        this.reportHistoryRepository = reportHistoryRepository;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
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
        return chronological(replyRepository.findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(reportId)).stream()
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
        ReplyType replyType = resolveAgentReplyType(request);
        if (replyType == ReplyType.COMPLEMENT_REQUEST) {
            if (isClosed(report)) {
                throw new BusinessException(
                        "Le signalement est clôturé : aucune demande de complément ne peut être envoyée.",
                        "REPORT_CLOSED");
            }
            if (!hasTrackedPassenger(report)) {
                throw new BusinessException(
                        "Une demande de complément n'est possible que pour un signalement avec suivi.",
                        "COMPLEMENT_NOT_ALLOWED");
            }
        }

        if (replyType != ReplyType.COMPLEMENT_REQUEST
                && request.getStatusId() != null
                && (report.getStatus() == null || !Objects.equals(report.getStatus().getStatusId(), request.getStatusId()))) {
            if (!permissionChecker.has("REPORT", "CLOSE") && !permissionChecker.has("REPORT", "EDIT")) {
                throw new BusinessException("Permission REPORT_CLOSE or REPORT_EDIT required to change status");
            }
            applyStatusChange(report, request.getStatusId(), user, request.getMessage());
        }

        if (replyType == ReplyType.COMPLEMENT_REQUEST) {
            applyComplementRequestStatus(report, user, request.getMessage());
        }

        if (request.getPublish() != null) {
            report.setPublish(request.getPublish());
            report.setPublishDate(Boolean.TRUE.equals(request.getPublish()) ? Instant.now() : null);
        }

        boolean publicResponse = request.getPublicResponse() == null || Boolean.TRUE.equals(request.getPublicResponse());
        if (replyType == ReplyType.INTERNAL_NOTE) {
            publicResponse = false;
        } else if (replyType == ReplyType.COMPLEMENT_REQUEST) {
            publicResponse = true;
        }
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
                .replyType(replyType)
                .authorType(ReplyAuthorType.AGENT)
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
                        + ";replyType=" + replyType
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
                            + " (— vérifiez les spams).")
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

    /**
     * Enregistre la réponse du voyageur à une demande de complément.
     * L'identité vient du jeton : le corps ne porte qu'un message.
     * Un signalement clôturé, ou sans demande encore ouverte, est refusé.
     * Plusieurs cycles restent possibles : une nouvelle demande agent rouvre le droit de répondre.
     */
    public ReplyResponse addPassengerComplement(UUID reportUuid, String message) {
        PassengerPrincipal principal = SecurityUtils.currentPassenger()
                .orElseThrow(() -> new BusinessException("Authentification requise.", "AUTH_REQUIRED"));
        if (!StringUtils.hasText(message) || message.trim().length() > 2000) {
            throw new BusinessException("Le message est invalide.", "REPLY_MESSAGE_INVALID");
        }
        Report report = reportRepository.findByUuid(reportUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportUuid));
        if (report.getPassenger() == null
                || !principal.getPassengerId().equals(report.getPassenger().getPassengerId())) {
            throw new BusinessException(
                    "Vous n'êtes pas autorisé à répondre à ce signalement.",
                    "REPLY_FORBIDDEN");
        }
        if (isClosed(report)) {
            throw new BusinessException("Le signalement est clôturé.", "REPORT_CLOSED");
        }
        List<Reply> existing = replyRepository.findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(report.getReportId());
        if (!hasPendingComplement(existing)) {
            throw new BusinessException(
                    "Aucune demande de complément n'est en attente.",
                    "COMPLEMENT_NOT_PENDING");
        }
        Passenger passenger = passengerRepository.findById(principal.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", principal.getPassengerId()));
        Reply reply = replyRepository.save(Reply.builder()
                .message(message.trim())
                .emailSent(false)
                .publicResponse(true)
                .replyType(ReplyType.COMPLEMENT_RESPONSE)
                .authorType(ReplyAuthorType.PASSENGER)
                .report(report)
                .passenger(passenger)
                .build());
        resumeAfterComplement(report, message.trim());
        report.setPublicResponse(true);
        report.setPublicResponseDate(Instant.now());
        reportRepository.save(report);
        return replyMapper.toResponse(reply);
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

        if (user == null) {
            return;
        }
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

    /**
     * Une demande de complément n'a de sens que si le voyageur peut revenir
     * sur le signalement avec son compte. Un dépôt anonyme, même avec un
     * e-mail de contact, ne donne pas ce suivi.
     */
    private static boolean hasTrackedPassenger(Report report) {
        Passenger passenger = report.getPassenger();
        return passenger != null && passenger.hasTrackedAccount();
    }

    /**
     * Un signalement est clôturé dès qu'une date de clôture est posée,
     * ou lorsque son statut est RESOLVED ou CLOSED.
     */
    public static boolean isClosed(Report report) {
        if (report.getClosureDate() != null) {
            return true;
        }
        return report.getStatus() != null && CLOSED_STATUS_CODES.contains(report.getStatus().getCode());
    }

    /**
     * Un complément est en attente lorsque le dernier message visible du voyageur
     * est une demande de complément. Une réponse de complément, ou tout autre
     * message public plus récent, referme le cycle. Une note interne est ignorée.
     */
    public static boolean hasPendingComplement(List<Reply> replies) {
        return chronological(replies).stream()
                .filter(Reply::isVisibleToPassenger)
                .reduce((first, second) -> second)
                .map(reply -> reply.effectiveType() == ReplyType.COMPLEMENT_REQUEST)
                .orElse(false);
    }

    /**
     * Ordre de conversation : date de message, puis identifiant technique.
     */
    public static List<Reply> chronological(List<Reply> replies) {
        List<Reply> ordered = new ArrayList<>(replies);
        ordered.sort(Comparator
                .comparing(Reply::getReplyDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Reply::getReplyId, Comparator.nullsLast(Comparator.naturalOrder())));
        return ordered;
    }

    /**
     * Passe le signalement en demande de complément et trace le changement
     * dans l'historique des statuts. Si le statut est déjà celui-ci, l'historique
     * n'est pas dupliqué.
     */
    private void applyComplementRequestStatus(Report report, AppUser user, String comments) {
        if (report.getStatus() != null && STATUS_COMPLEMENT.equals(report.getStatus().getCode())) {
            return;
        }
        Status waiting = statusRepository.findByCode(STATUS_COMPLEMENT)
                .orElseThrow(() -> new BusinessException(
                        "Statut DEMANDE_COMPLEMENT introuvable.", "STATUS_MISSING"));
        applyStatusChange(report, waiting.getStatusId(), user, comments);
    }

    /**
     * Après une réponse voyageur, le traitement reprend : le statut redevient
     * IN_PROGRESS et l'historique de statut enregistre le retour.
     */
    private void resumeAfterComplement(Report report, String comments) {
        if (report.getStatus() != null && STATUS_IN_PROGRESS.equals(report.getStatus().getCode())) {
            return;
        }
        Status inProgress = statusRepository.findByCode(STATUS_IN_PROGRESS)
                .orElseThrow(() -> new ResourceNotFoundException("Status", STATUS_IN_PROGRESS));
        applyStatusChange(report, inProgress.getStatusId(), null, comments);
    }

    private ReplyType resolveAgentReplyType(ReplyRequest request) {
        ReplyType type = request.getReplyType() == null ? ReplyType.RESPONSE : request.getReplyType();
        if (type == ReplyType.COMPLEMENT_RESPONSE) {
            throw new BusinessException(
                    "Ce type de message ne peut pas être créé par un agent.",
                    "REPLY_TYPE_FORBIDDEN");
        }
        return type;
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
