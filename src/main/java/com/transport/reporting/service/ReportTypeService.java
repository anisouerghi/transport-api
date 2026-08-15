package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.ReportTypeCriteria;
import com.transport.reporting.dto.ReportTypeRequest;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportTypeMapper;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.specification.ReportTypeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service metier ReportType (CRUD + recherche paginee + activation).
 */
@Service
@Transactional
public class ReportTypeService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "reportTypeId",
            "code", "code",
            "label", "label",
            "description", "description",
            "active", "active"
    );

    private final ReportTypeRepository reportTypeRepository;
    private final ReportTypeMapper reportTypeMapper;
    private final AuditLogService auditLogService;
    public ReportTypeService(ReportTypeRepository reportTypeRepository, ReportTypeMapper reportTypeMapper, AuditLogService auditLogService) {
        this.reportTypeRepository = reportTypeRepository;
        this.reportTypeMapper = reportTypeMapper;
        this.auditLogService = auditLogService;
    }


    @Transactional(readOnly = true)
    public List<ReportTypeResponse> findAll() {
        return reportTypeRepository.findAll().stream()
                .map(reportTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportTypeResponse> findAllActive() {
        ReportTypeCriteria criteria = new ReportTypeCriteria();
        criteria.setActive(true);
        return reportTypeRepository.findAll(ReportTypeSpecification.fromCriteria(criteria)).stream()
                .map(reportTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportTypeResponse findById(Long id) {
        return reportTypeMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportTypeResponse> search(SearchRequest<ReportTypeCriteria> request) {
        ReportTypeCriteria criteria = request.getFilters();
        Pageable pageable = PageableUtils.toPageable(request.getPageable(), "reportTypeId", SORT_FIELDS);
        Specification<ReportType> spec = ReportTypeSpecification.fromCriteria(criteria);
        Page<ReportTypeResponse> page = reportTypeRepository.findAll(spec, pageable)
                .map(reportTypeMapper::toResponse);
        return PageResponse.from(page);
    }

    public ReportTypeResponse create(ReportTypeRequest request) {
        if (reportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Report type code already exists");
        }
        ReportType entity = reportTypeRepository.save(reportTypeMapper.toEntity(request));
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.REPORT_TYPES)
                .entityName("ReportType")
                .entityId(String.valueOf(entity.getReportTypeId()))
                .newValue(snapshot(entity))
                .description("Création du type de signalement " + entity.getCode())
                .build());
        return reportTypeMapper.toResponse(entity);
    }

    public ReportTypeResponse update(Long id, ReportTypeRequest request) {
        ReportType entity = getEntity(id);
        String oldValue = snapshot(entity);
        if (!entity.getCode().equals(request.getCode())
                && reportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Report type code already exists");
        }
        reportTypeMapper.updateEntity(entity, request);
        entity = reportTypeRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.REPORT_TYPES)
                .entityName("ReportType")
                .entityId(String.valueOf(entity.getReportTypeId()))
                .oldValue(oldValue)
                .newValue(snapshot(entity))
                .description("Modification du type de signalement " + entity.getCode())
                .build());
        return reportTypeMapper.toResponse(entity);
    }

    public ReportTypeResponse setActive(Long id, boolean active) {
        ReportType entity = getEntity(id);
        String oldValue = "active=" + entity.isActive();
        entity.setActive(active);
        entity = reportTypeRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.REPORT_TYPES)
                .entityName("ReportType")
                .entityId(String.valueOf(entity.getReportTypeId()))
                .oldValue(oldValue)
                .newValue("active=" + entity.isActive())
                .description((active ? "Activation" : "Désactivation") + " du type " + entity.getCode())
                .build());
        return reportTypeMapper.toResponse(entity);
    }

    public void delete(Long id) {
        ReportType entity = getEntity(id);
        String oldValue = snapshot(entity);
        reportTypeRepository.deleteById(id);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.REPORT_TYPES)
                .entityName("ReportType")
                .entityId(String.valueOf(id))
                .oldValue(oldValue)
                .description("Suppression du type de signalement " + entity.getCode())
                .build());
    }

    ReportType getEntity(Long id) {
        return reportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportType", id));
    }

    private static String snapshot(ReportType entity) {
        return "code=" + entity.getCode()
                + ";label=" + entity.getLabel()
                + ";active=" + entity.isActive();
    }
}
