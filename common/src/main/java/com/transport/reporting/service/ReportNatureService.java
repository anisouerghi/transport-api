package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReportNatureCriteria;
import com.transport.reporting.dto.ReportNatureRequest;
import com.transport.reporting.dto.ReportNatureResponse;
import com.transport.reporting.entity.ReportNature;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportNatureMapper;
import com.transport.reporting.repository.ReportNatureRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.specification.ReportNatureSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service métier des natures de signalement (référentiel + activation).
 */
@Service
@Transactional
public class ReportNatureService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "reportNatureId",
            "code", "code",
            "label", "label",
            "description", "description",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final ReportNatureRepository reportNatureRepository;
    private final ReportRepository reportRepository;
    private final ReportNatureMapper reportNatureMapper;
    private final AuditLogService auditLogService;

    public ReportNatureService(
            ReportNatureRepository reportNatureRepository,
            ReportRepository reportRepository,
            ReportNatureMapper reportNatureMapper,
            AuditLogService auditLogService) {
        this.reportNatureRepository = reportNatureRepository;
        this.reportRepository = reportRepository;
        this.reportNatureMapper = reportNatureMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ReportNatureResponse> findAll() {
        return reportNatureRepository.findAll().stream()
                .map(reportNatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportNatureResponse> findAllActive() {
        return reportNatureRepository.findByActiveTrueOrderByLabelAsc().stream()
                .map(reportNatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportNatureResponse findById(Long id) {
        return reportNatureMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportNatureResponse> search(SearchRequest<ReportNatureCriteria> request) {
        ReportNatureCriteria criteria = request != null ? request.getFilters() : null;
        Pageable pageable = PageableUtils.toPageable(
                request != null ? request.getPageable() : null,
                "label",
                SORT_FIELDS);
        Specification<ReportNature> spec = ReportNatureSpecification.fromCriteria(criteria);
        Page<ReportNatureResponse> page = reportNatureRepository.findAll(spec, pageable)
                .map(reportNatureMapper::toResponse);
        return PageResponse.from(page);
    }

    public ReportNatureResponse create(ReportNatureRequest request) {
        if (reportNatureRepository.existsByCode(normalizeCode(request.getCode()))) {
            throw new BusinessException("Nature code already exists");
        }
        ReportNature entity = reportNatureRepository.save(reportNatureMapper.toEntity(request));
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.REPORT_NATURES)
                .entityName("ReportNature")
                .entityId(String.valueOf(entity.getReportNatureId()))
                .newValue(snapshot(entity))
                .description("Création de la nature " + entity.getCode())
                .build());
        return reportNatureMapper.toResponse(entity);
    }

    public ReportNatureResponse update(Long id, ReportNatureRequest request) {
        ReportNature entity = getEntity(id);
        String oldValue = snapshot(entity);
        String newCode = normalizeCode(request.getCode());
        if (!entity.getCode().equalsIgnoreCase(newCode) && reportNatureRepository.existsByCode(newCode)) {
            throw new BusinessException("Nature code already exists");
        }
        reportNatureMapper.updateEntity(entity, request);
        entity = reportNatureRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.REPORT_NATURES)
                .entityName("ReportNature")
                .entityId(String.valueOf(entity.getReportNatureId()))
                .oldValue(oldValue)
                .newValue(snapshot(entity))
                .description("Modification de la nature " + entity.getCode())
                .build());
        return reportNatureMapper.toResponse(entity);
    }

    public ReportNatureResponse setActive(Long id, boolean active) {
        ReportNature entity = getEntity(id);
        String oldValue = "active=" + entity.isActive();
        entity.setActive(active);
        entity = reportNatureRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.REPORT_NATURES)
                .entityName("ReportNature")
                .entityId(String.valueOf(entity.getReportNatureId()))
                .oldValue(oldValue)
                .newValue("active=" + entity.isActive())
                .description((active ? "Activation" : "Désactivation") + " de la nature " + entity.getCode())
                .build());
        return reportNatureMapper.toResponse(entity);
    }

    public void delete(Long id) {
        ReportNature entity = getEntity(id);
        if (reportRepository.existsByNature_ReportNatureId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer cette nature : des signalements y sont encore rattachés. "
                            + "Désactivez-la plutôt.");
        }
        String oldValue = snapshot(entity);
        reportNatureRepository.deleteById(id);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.REPORT_NATURES)
                .entityName("ReportNature")
                .entityId(String.valueOf(id))
                .oldValue(oldValue)
                .description("Suppression de la nature " + entity.getCode())
                .build());
    }

    ReportNature getEntity(Long id) {
        return reportNatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportNature", id));
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase().replace(' ', '_');
    }

    private static String snapshot(ReportNature entity) {
        return "code=" + entity.getCode()
                + ";label=" + entity.getLabel()
                + ";active=" + entity.isActive();
    }
}
