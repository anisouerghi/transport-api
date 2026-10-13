package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.EmailSendResult;
import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.ReportNature;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportHistory;
import com.transport.reporting.entity.ReportSatisfaction;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportMapper;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportNatureRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportSatisfactionRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.security.SecurityUtils;
import com.transport.reporting.specification.ReportSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service métier Signalement (création publique, recherche admin, détail).
 */
@Service
@Transactional
public class ReportService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "reportId",
            "reference", "reference",
            "creationDate", "creationDate",
            "closureDate", "closureDate",
            "priority", "priority"
    );

    private final ReportRepository reportRepository;
    private final ReportTypeRepository reportTypeRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final UserRepository userRepository;
    private final PassengerService passengerService;
    private final StatusService statusService;
    private final ReportMapper reportMapper;
    private final AttachmentService attachmentService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final ReplyEmailComposer replyEmailComposer;
    private final ReplyRepository replyRepository;
    private final ReportNatureRepository reportNatureRepository;
    private final ReportSatisfactionRepository satisfactionRepository;

    public ReportService(ReportRepository reportRepository, ReportTypeRepository reportTypeRepository, TransportSupportRepository transportSupportRepository, ReportHistoryRepository reportHistoryRepository, UserRepository userRepository, PassengerService passengerService, StatusService statusService, ReportMapper reportMapper, AttachmentService attachmentService, FileStorageService fileStorageService, AuditLogService auditLogService, EmailService emailService, ReplyEmailComposer replyEmailComposer, ReplyRepository replyRepository, ReportNatureRepository reportNatureRepository, ReportSatisfactionRepository satisfactionRepository) {
        this.reportRepository = reportRepository;
        this.reportTypeRepository = reportTypeRepository;
        this.transportSupportRepository = transportSupportRepository;
        this.reportHistoryRepository = reportHistoryRepository;
        this.userRepository = userRepository;
        this.passengerService = passengerService;
        this.statusService = statusService;
        this.reportMapper = reportMapper;
        this.attachmentService = attachmentService;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.replyEmailComposer = replyEmailComposer;
        this.replyRepository = replyRepository;
        this.reportNatureRepository = reportNatureRepository;
        this.satisfactionRepository = satisfactionRepository;
    }


    /**
     * Crée un signalement sans pièce jointe (compatibilité appels internes / JSON pur).
     */
    public ReportResponse create(ReportRequest request) {
        return create(request, null);
    }

    /**
     * Crée un signalement puis rattache éventuellement des pièces jointes.
     * <p>
     * Les fichiers sont validés <strong>avant</strong> la persistence du signalement
     * afin d'éviter un enregistrement orphelin en cas de rejet d'upload.
     *
     * @param request données métier du signalement
     * @param files   fichiers multipart optionnels (max 5)
     */
    public ReportResponse create(ReportRequest request, MultipartFile[] files) {
        // Valider les fichiers avant toute persistence
        fileStorageService.validateBatch(files);

        TransportSupport support = request.getSupportUuid() == null
            ? null
            : transportSupportRepository.findByUuid(request.getSupportUuid())
                .filter(s -> s.getSupportStatus() == SupportStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Active TransportSupport", request.getSupportUuid()));

        ReportType reportType = request.getReportTypeId() == null
            ? null
            : reportTypeRepository.findById(request.getReportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ReportType", request.getReportTypeId()));

        Passenger passenger = resolvePassenger(request.getPassenger());
        Status initialStatus = statusService.findByCode("NEW");

        Report report = Report.builder()
                .reference(generateReference())
                .description(request.getDescription())
                .priority(Priority.MEDIUM)
                .publish(Boolean.FALSE)
                .sendEmail(Boolean.FALSE)
                .publicResponse(Boolean.FALSE)
                .transportSupport(support)
                .reportType(reportType)
                .passenger(passenger)
                .status(initialStatus)
                .build();

        report = reportRepository.save(report);
        ReportResponse response = reportMapper.toResponse(report);
        response.setAttachments(attachmentService.saveForReport(report, files));
        sendCreationEmailIfPossible(report, passenger);
        sanitizePublicCreateResponse(response);

        String passengerName = passenger == null || passenger.getName() == null
            ? "voyageur"
            : passenger.getName();
        auditLogService.record(AuditLogEvent.builder()
                .username("PUBLIC")
                .userFullName(passengerName)
                .actionType(AuditAction.CREATE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .newValue("reference=" + report.getReference()
                    + ";reportTypeId=" + (reportType == null ? null : reportType.getReportTypeId()))
                .description("Création publique du signalement " + report.getReference())
                .build());

        return response;
    }

    /**
     * Met à jour la priorité interne d'un signalement (agents / administrateurs).
     * Trace l'action dans {@code report_history} et le journal d'audit.
     */
    public ReportResponse updatePriority(Long id, Priority newPriority) {
        if (newPriority == null) {
            throw new BusinessException("Priority is required");
        }
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        refuseIfClosed(report);

        Priority previous = report.getPriority();
        if (previous == newPriority) {
            return toResponseWithAttachments(report);
        }

        report.setPriority(newPriority);
        report = reportRepository.save(report);

        AppUser actor = resolveCurrentUser();
        Status currentStatus = report.getStatus();
        if (currentStatus == null) {
            currentStatus = statusService.findByCode("NEW");
        }
        String comment = "Priorité : "
                + (previous != null ? previous.name() : "null")
                + " → " + newPriority.name();
        reportHistoryRepository.save(ReportHistory.builder()
                .oldStatus(currentStatus)
                .newStatus(currentStatus)
                .comments(comment)
                .report(report)
                .appUser(actor)
                .build());

        auditLogService.record(AuditLogEvent.builder()
                .userId(actor != null ? actor.getUserId() : AuditActors.currentAdminUserId())
                .username(actor != null ? actor.getUsername() : null)
                .userFullName(actor != null ? actor.getName() : null)
                .actionType(AuditAction.PRIORITY_CHANGE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(id))
                .oldValue("priority=" + previous)
                .newValue("priority=" + newPriority)
                .description("Modification de priorité du signalement " + report.getReference())
                .build());

        return toResponseWithAttachments(report);
    }

    /**
     * Affecte ou retire la nature métier d'un signalement ({@code REPORT_ASSIGN_NATURE}).
     */
    public ReportResponse updateNature(Long id, Long reportNatureId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        refuseIfClosed(report);

        ReportNature previous = report.getNature();
        Long previousId = previous != null ? previous.getReportNatureId() : null;
        if ((previousId == null && reportNatureId == null)
                || (previousId != null && previousId.equals(reportNatureId))) {
            return toResponseWithAttachments(report);
        }

        ReportNature next = null;
        if (reportNatureId != null) {
            next = reportNatureRepository.findById(reportNatureId)
                    .orElseThrow(() -> new ResourceNotFoundException("ReportNature", reportNatureId));
            if (!next.isActive()) {
                throw new BusinessException("Cannot assign an inactive nature");
            }
        }

        report.setNature(next);
        report = reportRepository.save(report);

        AppUser actor = resolveCurrentUser();
        Status currentStatus = report.getStatus();
        if (currentStatus == null) {
            currentStatus = statusService.findByCode("NEW");
        }
        String oldLabel = previous != null ? previous.getLabel() : "Non classé";
        String newLabel = next != null ? next.getLabel() : "Non classé";
        reportHistoryRepository.save(ReportHistory.builder()
                .oldStatus(currentStatus)
                .newStatus(currentStatus)
                .comments("Nature : " + oldLabel + " → " + newLabel)
                .report(report)
                .appUser(actor)
                .build());

        auditLogService.record(AuditLogEvent.builder()
                .userId(actor != null ? actor.getUserId() : AuditActors.currentAdminUserId())
                .username(actor != null ? actor.getUsername() : null)
                .userFullName(actor != null ? actor.getName() : null)
                .actionType(AuditAction.NATURE_CHANGE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(id))
                .oldValue("nature=" + (previous != null ? previous.getCode() : "null"))
                .newValue("nature=" + (next != null ? next.getCode() : "null"))
                .description("Modification de nature du signalement " + report.getReference()
                        + " (" + oldLabel + " → " + newLabel + ")")
                .build());

        return toResponseWithAttachments(report);
    }

    /**
     * Met à jour le type ({@code ReportType}) d'un signalement — natures voyageur.
     * {@code reportTypeId} null = retirer le type.
     */
    public ReportResponse updateReportType(Long id, Long reportTypeId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        refuseIfClosed(report);

        ReportType previous = report.getReportType();
        Long previousId = previous != null ? previous.getReportTypeId() : null;
        if ((previousId == null && reportTypeId == null)
                || (previousId != null && previousId.equals(reportTypeId))) {
            return toResponseWithAttachments(report);
        }

        ReportType next = null;
        if (reportTypeId != null) {
            next = reportTypeRepository.findById(reportTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("ReportType", reportTypeId));
            if (!next.isActive()) {
                throw new BusinessException("Cannot assign an inactive report type");
            }
        }

        report.setReportType(next);
        report = reportRepository.save(report);

        AppUser actor = resolveCurrentUser();
        Status currentStatus = report.getStatus();
        if (currentStatus == null) {
            currentStatus = statusService.findByCode("NEW");
        }
        String oldLabel = previous != null ? previous.getLabel() : "Non définie";
        String newLabel = next != null ? next.getLabel() : "Non définie";
        reportHistoryRepository.save(ReportHistory.builder()
                .oldStatus(currentStatus)
                .newStatus(currentStatus)
                .comments("Type : " + oldLabel + " → " + newLabel)
                .report(report)
                .appUser(actor)
                .build());

        auditLogService.record(AuditLogEvent.builder()
                .userId(actor != null ? actor.getUserId() : AuditActors.currentAdminUserId())
                .username(actor != null ? actor.getUsername() : null)
                .userFullName(actor != null ? actor.getName() : null)
                .actionType(AuditAction.REPORT_TYPE_CHANGE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(id))
                .oldValue("reportType=" + (previous != null ? previous.getCode() : "null"))
                .newValue("reportType=" + (next != null ? next.getCode() : "null"))
                .description("Modification du type du signalement " + report.getReference()
                        + " (" + oldLabel + " → " + newLabel + ")")
                .build());

        return toResponseWithAttachments(report);
    }

    private AppUser resolveCurrentUser() {
        Long userId = SecurityUtils.currentUserIdOrNull();
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public ReportResponse findByReference(String reference) {
        Report report = reportRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reference));
        ReportResponse response = toResponseWithAttachments(report);
        // Priorité réservée au traitement interne
        response.setPriority(null);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll() {
        List<Report> reports = reportRepository.findAll();
        Map<Long, ReportSatisfaction> satisfactions = satisfactionsByReportId(
                reports.stream().map(Report::getReportId).toList());
        return reports.stream().map(report -> {
            ReportResponse dto = reportMapper.toResponse(report);
            applySatisfaction(dto, satisfactions.get(report.getReportId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> search(SearchRequest<ReportCriteria> request) {
        ReportCriteria criteria = request != null ? request.getFilters() : null;
        Pageable pageable = PageableUtils.toPageable(
                request != null ? request.getPageable() : null,
                "reportId",
                SORT_FIELDS
        );
        Specification<Report> spec = ReportSpecification.fromCriteria(criteria);
        Page<Report> entityPage = reportRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(Report::getReportId).toList();
        Set<Long> repliedIds = repliedReportIds(ids);
        Map<Long, ReportSatisfaction> satisfactions = satisfactionsByReportId(ids);
        Page<ReportResponse> page = entityPage.map(report -> {
            ReportResponse dto = reportMapper.toResponse(report);
            dto.setReplied(repliedIds.contains(report.getReportId()));
            applySatisfaction(dto, satisfactions.get(report.getReportId()));
            return dto;
        });
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ReportResponse findById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        return toResponseWithAttachments(report);
    }

    private ReportResponse toResponseWithAttachments(Report report) {
        ReportResponse response = reportMapper.toResponse(report);
        response.setAttachments(attachmentService.findByReportId(report.getReportId()));
        response.setReplied(replyRepository.existsByReport_ReportId(report.getReportId()));
        satisfactionRepository.findByReport_ReportId(report.getReportId())
                .ifPresent(satisfaction -> applySatisfaction(response, satisfaction));
        return response;
    }

    private Map<Long, ReportSatisfaction> satisfactionsByReportId(List<Long> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Map.of();
        }
        return satisfactionRepository.findByReport_ReportIdIn(reportIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        satisfaction -> satisfaction.getReport().getReportId(),
                        satisfaction -> satisfaction,
                        (left, right) -> left));
    }

    private static void applySatisfaction(ReportResponse response, ReportSatisfaction satisfaction) {
        if (satisfaction == null || satisfaction.getLevel() == null) {
            return;
        }
        response.setSatisfactionLevel(satisfaction.getLevel().name());
        response.setSatisfactionComment(satisfaction.getComment());
        response.setSatisfactionCreatedAt(satisfaction.getCreatedAt());
    }

    private static void refuseIfClosed(Report report) {
        if (ReplyService.isClosed(report)) {
            throw new BusinessException(
                    "Le signalement est clôturé et ne peut plus être modifié.",
                    "REPORT_CLOSED");
        }
    }

    private Set<Long> repliedReportIds(List<Long> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(replyRepository.findReportIdsHavingReplies(reportIds));
    }

    /**
     * Génère une référence unique du type {@code SIG<AA>-<NNNN>-<XXXX>} :
     * année sur 2 chiffres, numéro séquentiel annuel sur 4 chiffres (zéros à
     * gauche) et suffixe aléatoire de 4 caractères {@code [0-9A-Z]}.
     * Exemple : {@code SIG26-0001-K8YA}.
     *
     * <p>La séquence repart à 1 le 1er janvier (elle est déduite de la plus
     * grande référence existante de l'année). Le suffixe aléatoire garantit
     * l'unicité ; en cas de collision (même séquence + même suffixe déjà pris),
     * on avance à la séquence suivante.</p>
     */
    private String generateReference() {
        String yearPrefix = "SIG" + String.format("%02d", LocalDate.now().getYear() % 100) + "-";
        long next = reportRepository.findTopByReferenceStartingWithOrderByReferenceDesc(yearPrefix)
                .map(last -> parseSequence(last.getReference(), yearPrefix.length()) + 1)
                .orElse(1L);
        while (true) {
            String candidate = yearPrefix + String.format("%04d", next) + "-" + randomSuffix(4);
            if (!reportRepository.existsByReference(candidate)) {
                return candidate;
            }
            next++;
        }
    }

    /** Extrait la séquence sur 4 chiffres d'une référence {@code SIG<AA>-<NNNN>-...}. */
    private static long parseSequence(String reference, int prefixLength) {
        if (reference == null || reference.length() < prefixLength + 4) {
            return 0L;
        }
        try {
            return Long.parseLong(reference.substring(prefixLength, prefixLength + 4));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** Suffixe aléatoire de {@code length} caractères parmi {@code [0-9A-Z]}. */
    private static String randomSuffix(int length) {
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder suffix = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return suffix.toString();
    }

    /**
     * Envoie le lien de suivi au voyageur dès la création, si un e-mail est renseigné.
     * L'échec SMTP n'empêche pas l'enregistrement du signalement.
     */
    private void sendCreationEmailIfPossible(Report report, Passenger passenger) {
        if (passenger == null || !StringUtils.hasText(passenger.getEmail())) {
            return;
        }
        String to = passenger.getEmail().trim();
        EmailSendResult result = emailService.sendHtml(
                to,
                replyEmailComposer.confirmationSubject(),
                replyEmailComposer.buildConfirmationHtml(report));
        if (result.isSuccess()) {
            report.setSendEmail(true);
            report.setSendEmailDate(java.time.Instant.now());
            reportRepository.save(report);
        }
        auditLogService.record(AuditLogEvent.builder()
                .username("PUBLIC")
                .userFullName(passenger.getName())
                .actionType(AuditAction.EMAIL_SEND)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .result(result.isSuccess() ? AuditResult.SUCCESS : AuditResult.FAILURE)
                .newValue("to=" + to + (result.isSuccess() ? "" : ";error=" + result.getMessage()))
                .description(result.isSuccess()
                        ? "E-mail de confirmation envoyé pour " + report.getReference()
                        : "Échec e-mail de confirmation pour " + report.getReference())
                .build());
    }

    /**
     * Voyageur authentifié : rattache le signalement au compte.
     * Anonyme : find-or-create (e-mail optionnel).
     */
    private Passenger resolvePassenger(PassengerRequest request) {
        return SecurityUtils.currentPassenger()
            .map(principal -> passengerService.getEntity(principal.getPassengerId()))
            .orElseGet(() -> request == null ? null : passengerService.findOrCreate(request));
    }

    /**
     * Masque les identifiants sensibles (UUID, ID interne) dans la réponse publique de création.
     * Le suivi détaillé n'est accessible que via le lien e-mail sécurisé.
     */
    private static void sanitizePublicCreateResponse(ReportResponse response) {
        response.setReportId(null);
        response.setUuid(null);
        response.setPriority(null);
        response.setPublish(null);
        response.setPublishDate(null);
        response.setSendEmail(null);
        response.setSendEmailDate(null);
        response.setPublicResponse(null);
        response.setPublicResponseDate(null);
    }
}
