package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportHistory;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportMapper;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.security.SecurityUtils;
import com.transport.reporting.specification.ReportSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service métier Signalement (création publique, recherche admin, détail).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

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

        TransportSupport support = transportSupportRepository.findByUuid(request.getSupportUuid())
                .filter(s -> s.getSupportStatus() == SupportStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active TransportSupport", request.getSupportUuid()));

        ReportType reportType = reportTypeRepository.findById(request.getReportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ReportType", request.getReportTypeId()));

        Passenger passenger = passengerService.findOrCreate(request.getPassenger());
        Status initialStatus = statusService.findByCode("NEW");

        Report report = Report.builder()
                .reference(generateReference())
                .description(request.getDescription())
                // Priorité interne : défaut métier (à évaluer / normale). Jamais saisie par le voyageur.
                .priority(Priority.MEDIUM)
                .publish(request.getPublish() != null ? request.getPublish() : Boolean.FALSE)
                .publishDate(request.getPublishDate())
                .sendEmail(request.getSendEmail() != null ? request.getSendEmail() : Boolean.FALSE)
                .sendEmailDate(request.getSendEmailDate())
                .publicResponse(request.getPublicResponse() != null ? request.getPublicResponse() : Boolean.FALSE)
                .publicResponseDate(request.getPublicResponseDate())
                .transportSupport(support)
                .reportType(reportType)
                .passenger(passenger)
                .status(initialStatus)
                .build();

        report = reportRepository.save(report);
        ReportResponse response = reportMapper.toResponse(report);
        response.setAttachments(attachmentService.saveForReport(report, files));
        // Ne pas exposer la priorité au canal public
        response.setPriority(null);

        String passengerName = passenger.getName() != null ? passenger.getName() : "voyageur";
        auditLogService.record(AuditLogEvent.builder()
                .username("PUBLIC")
                .userFullName(passengerName)
                .actionType(AuditAction.CREATE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .newValue("reference=" + report.getReference()
                        + ";reportTypeId=" + reportType.getReportTypeId())
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
        return reportRepository.findAll().stream().map(reportMapper::toResponse).toList();
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
        Page<ReportResponse> page = reportRepository.findAll(spec, pageable)
                .map(reportMapper::toResponse);
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
        return response;
    }

    /** Génère une référence unique du type {@code SIG-yyyyMMdd-xxxxxx}. */
    private String generateReference() {
        String reference;
        do {
            int random = ThreadLocalRandom.current().nextInt(100000, 999999);
            reference = "SIG-" + LocalDate.now().format(DATE_FORMAT) + "-" + random;
        } while (reportRepository.existsByReference(reference));
        return reference;
    }
}
