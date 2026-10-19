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
    /** Demande d'info voyageur : complément et clarification partagent le même cycle métier. */
    private static final Set<String> COMPLEMENT_STATUS_CODES = Set.of(
            "DEMANDE_COMPLEMENT", "DEMANDE_CLARIFICATION");
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
    private final ReportTypeAccessService reportTypeAccessService;
    private final PassengerEmailNotificationGuard emailNotificationGuard;
    private final PublicStatusMapper publicStatusMapper;

    public ReplyService(
            ReplyRepository replyRepository,
            ReportRepository reportRepository,
            ReportHistoryRepository reportHistoryRepository,
            UserRepository userRepository,
            PassengerRepository passengerRepository,
            StatusRepository statusRepository,
            ReplyMapper replyMapper,
            AuditLogService auditLogService,
            PermissionChecker permissionChecker,
            EmailService emailService,
            ReplyEmailComposer replyEmailComposer,
            ReportTypeAccessService reportTypeAccessService,
            PassengerEmailNotificationGuard emailNotificationGuard,
            PublicStatusMapper publicStatusMapper) {
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
        this.reportTypeAccessService = reportTypeAccessService;
        this.emailNotificationGuard = emailNotificationGuard;
        this.publicStatusMapper = publicStatusMapper;
    }


    @Transactional(readOnly = true)
    public List<ReplyResponse> findByReportId(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        reportTypeAccessService.requireAccess(report);
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
        reportTypeAccessService.requireAccess(report);
        if (isClosed(report)) {
            throw new BusinessException(
                    "Le signalement est clôturé et ne peut plus être modifié.",
                    "REPORT_CLOSED");
        }

        AppUser user = resolveActor(request.getUserId());
        Status previousStatus = report.getStatus();
        String previousPublic = publicStatusMapper.publicLabel(previousStatus);
        // Le type de message n'est plus saisi : il découle du statut cible (et d'une note interne explicite).
        ReplyType replyType = resolveAgentReplyType(request, request.getStatusId());
        if (replyType == ReplyType.COMPLEMENT_REQUEST) {
            if (!hasTrackedPassenger(report)) {
                throw new BusinessException(
                        "Une demande de complément n'est possible que pour un signalement avec suivi.",
                        "COMPLEMENT_NOT_ALLOWED");
            }
            if (!StringUtils.hasText(request.getMessage())) {
                throw new BusinessException(
                        "Le message est obligatoire pour une demande de complément.",
                        "COMPLEMENT_MESSAGE_REQUIRED");
            }
        }

        boolean statusChanged = false;
        if (replyType == ReplyType.COMPLEMENT_REQUEST) {
            Long beforeId = report.getStatus() != null ? report.getStatus().getStatusId() : null;
            // Conserve DEMANDE_CLARIFICATION si choisi ; sinon DEMANDE_COMPLEMENT par défaut.
            applyComplementRequestStatus(report, user, request.getMessage(), request.getStatusId());
            statusChanged = !Objects.equals(beforeId,
                    report.getStatus() != null ? report.getStatus().getStatusId() : null);
        } else if (request.getStatusId() != null
                && (report.getStatus() == null || !Objects.equals(report.getStatus().getStatusId(), request.getStatusId()))) {
            if (!permissionChecker.has("REPORT", "CLOSE") && !permissionChecker.has("REPORT", "EDIT")) {
                throw new BusinessException("Permission REPORT_CLOSE or REPORT_EDIT required to change status");
            }
            applyStatusChange(report, request.getStatusId(), user, request.getMessage());
            statusChanged = true;
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

        String newPublic = publicStatusMapper.publicLabel(report.getStatus());
        // AFFECTE → IN_PROGRESS : même statut public → pas de notification de transition.
        boolean notifyStatusTransition = statusChanged
                && publicStatusMapper.publicStatusChanged(previousPublic, newPublic)
                && publicStatusMapper.publicStatusChanged(report.getLastNotifiedPublicStatus(), newPublic);
        // Nouvelle demande de complément avec message, même si le statut interne était déjà celui-ci.
        boolean notifyComplementMessage = replyType == ReplyType.COMPLEMENT_REQUEST && publicResponse;

        String passengerEmail = resolvePassengerEmail(report);
        // E-mail si message public (comportement historique) ou transition de statut public.
        boolean emailRequested = replyType != ReplyType.INTERNAL_NOTE
                && publicResponse
                && !Boolean.FALSE.equals(request.getSendEmail());
        boolean canSendEmail = emailRequested && isDeliverableEmail(passengerEmail);

        String agentMessage = request.getMessage().trim();
        Reply reply = Reply.builder()
                .message(agentMessage)
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
            PassengerEmailNotificationGuard.Decision emailDecision =
                    emailNotificationGuard.evaluate(report.getPassenger());
            if (emailDecision != PassengerEmailNotificationGuard.Decision.ALLOW) {
                log.info(
                        "Notification e-mail ignorée pour report {} (code={})",
                        reportId,
                        emailDecision.errorCode());
                emailResult = EmailSendResult.fail(emailDecision.errorCode(), emailDecision.message());
                auditEmail(user, report, passengerEmail, false, emailDecision.message());
            } else {
                try {
                    String mailBody = resolveNotificationBody(
                            agentMessage, notifyStatusTransition || notifyComplementMessage, newPublic);
                    emailResult = sendReplyEmail(report, reply, user, passengerEmail, newPublic, mailBody);
                    if (emailResult.isSuccess() && (notifyStatusTransition || notifyComplementMessage)
                            && StringUtils.hasText(newPublic)) {
                        // Mémorise le statut public notifié pour bloquer les doublons AFFECTE/EN_COURS.
                        report.setLastNotifiedPublicStatus(newPublic);
                    }
                } catch (Exception ex) {
                    log.error("Echec technique envoi e-mail pour report {}", reportId, ex);
                    emailResult = EmailSendResult.fail("EMAIL_SEND_FAILED",
                            "L'e-mail n'a pas pu être envoyé. Détail : " + ex.getMessage());
                }
            }
        } else if (!publicResponse && Boolean.TRUE.equals(request.getSendEmail())) {
            emailResult = EmailSendResult.fail(
                    "EMAIL_NOT_PUBLIC",
                    "Réponse enregistrée, mais l'e-mail n'a pas été envoyé car la visibilité voyageur est désactivée."
            );
            log.info("sendEmail requested for report {} but publicResponse=false", reportId);
            auditEmail(user, report, passengerEmail, false, emailResult.getMessage());
        } else if (publicResponse && Boolean.FALSE.equals(request.getSendEmail())) {
            emailResult = null;
        } else if (publicResponse && !isDeliverableEmail(passengerEmail)) {
            emailResult = EmailSendResult.fail("EMAIL_NO_RECIPIENT",
                    "La réponse a été enregistrée, mais aucun e-mail voyageur n'est renseigné : aucun envoi effectué.");
            log.warn("Réponse visible pour le report {} sans adresse e-mail voyageur", reportId);
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

        // Préférence désactivée / indisponible : la réponse reste un succès métier.
        if (PassengerEmailNotificationGuard.SKIP_CODE.equals(emailResult.getErrorCode())
                || PassengerEmailNotificationGuard.ERROR_SKIP_CODE.equals(emailResult.getErrorCode())) {
            return ReplyCreateResult.builder()
                    .reply(response)
                    .replySaved(true)
                    .success(true)
                    .message("Réponse enregistrée. " + emailResult.getMessage())
                    .errorCode(emailResult.getErrorCode())
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

    /**
     * Commentaire libre hors avis : refusé. Le commentaire du voyageur fait partie
     * de l'avis, et seulement lorsque le signalement est clôturé.
     */
    public ReplyResponse addPassengerComment(UUID reportUuid, String message) {
        throw new BusinessException(
                "Le commentaire accompagne l'avis, uniquement pour un signalement clôturé.",
                "COMMENT_RESERVED_FOR_CLOSED");
    }

    private EmailSendResult sendReplyEmail(
            Report report,
            Reply reply,
            AppUser user,
            String passengerEmail,
            String publicStatusLabel,
            String notificationBody) {
        String html = replyEmailComposer.buildHtml(report, reply, publicStatusLabel, notificationBody);
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

    /**
     * Corps de la notification : message agent s'il est public, sinon texte adapté au statut public.
     * Jamais une note interne.
     */
    private String resolveNotificationBody(String agentMessage, boolean statusOriented, String publicLabel) {
        if (StringUtils.hasText(agentMessage)) {
            return agentMessage.trim();
        }
        if (statusOriented) {
            return publicStatusMapper.defaultNotificationMessage(publicLabel);
        }
        return agentMessage;
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
        if (passenger == null || !StringUtils.hasText(passenger.getEmail())) {
            return null;
        }
        String email = passenger.getEmail().trim();
        return isDeliverableEmail(email) ? email : null;
    }

    private static boolean isDeliverableEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1 && email.indexOf('@', at + 1) < 0
                && !email.contains(" ") && email.indexOf('.', at) > at + 1;
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
     * Passe le signalement en demande de complément ou de clarification.
     * Les deux codes ouvrent le même cycle voyageur ({@link ReplyType#COMPLEMENT_REQUEST}).
     * Si le statut cible est déjà atteint, l'historique n'est pas dupliqué.
     */
    private void applyComplementRequestStatus(Report report, AppUser user, String comments, Long requestedStatusId) {
        Status waiting = resolveComplementTargetStatus(requestedStatusId);
        if (report.getStatus() != null
                && Objects.equals(report.getStatus().getStatusId(), waiting.getStatusId())) {
            return;
        }
        applyStatusChange(report, waiting.getStatusId(), user, comments);
    }

    private Status resolveComplementTargetStatus(Long requestedStatusId) {
        if (requestedStatusId != null) {
            Status requested = statusRepository.findById(requestedStatusId)
                    .orElseThrow(() -> new ResourceNotFoundException("Status", requestedStatusId));
            if (isComplementStatus(requested)) {
                return requested;
            }
        }
        return statusRepository.findByCode(STATUS_COMPLEMENT)
                .orElseThrow(() -> new BusinessException(
                        "Statut DEMANDE_COMPLEMENT introuvable.", "STATUS_MISSING"));
    }

    static boolean isComplementStatus(Status status) {
        return status != null && status.getCode() != null
                && COMPLEMENT_STATUS_CODES.contains(status.getCode());
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

    /**
     * Déduit le type d'événement : {@code DEMANDE_COMPLEMENT} ou {@code DEMANDE_CLARIFICATION}
     * → demande d'information voyageur. {@code INTERNAL_NOTE} reste possible si envoyé explicitement.
     */
    private ReplyType resolveAgentReplyType(ReplyRequest request, Long statusId) {
        ReplyType type = request.getReplyType() == null ? ReplyType.RESPONSE : request.getReplyType();
        if (type == ReplyType.COMPLEMENT_RESPONSE) {
            throw new BusinessException(
                    "Ce type de message ne peut pas être créé par un agent.",
                    "REPLY_TYPE_FORBIDDEN");
        }
        if (type == ReplyType.INTERNAL_NOTE) {
            return type;
        }
        if (statusId != null) {
            Status target = statusRepository.findById(statusId).orElse(null);
            if (isComplementStatus(target)) {
                return ReplyType.COMPLEMENT_REQUEST;
            }
        }
        if (type == ReplyType.COMPLEMENT_REQUEST) {
            return ReplyType.COMPLEMENT_REQUEST;
        }
        return ReplyType.RESPONSE;
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
