package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportMapper;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
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
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
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

        String passengerName = passenger.getName() != null ? passenger.getName() : "voyageur";
        auditLogService.record(AuditLogEvent.builder()
                .username("PUBLIC")
                .userFullName(passengerName)
                .actionType(AuditAction.CREATE)
                .module(AuditModule.REPORTS)
                .entityName("Report")
                .entityId(String.valueOf(report.getReportId()))
                .newValue("reference=" + report.getReference()
                        + ";priority=" + report.getPriority()
                        + ";reportTypeId=" + reportType.getReportTypeId())
                .description("Création publique du signalement " + report.getReference())
                .build());

        return response;
    }

    @Transactional(readOnly = true)
    public ReportResponse findByReference(String reference) {
        Report report = reportRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reference));
        return toResponseWithAttachments(report);
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
