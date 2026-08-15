package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.SupportTypeCriteria;
import com.transport.reporting.dto.SupportTypeRequest;
import com.transport.reporting.dto.SupportTypeResponse;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.SupportTypeMapper;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.specification.SupportTypeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service metier SupportType (CRUD + recherche paginee serveur).
 */
@Service
@Transactional
public class SupportTypeService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "supportTypeId",
            "code", "code",
            "label", "label"
    );

    private final SupportTypeRepository supportTypeRepository;
    private final SupportTypeMapper supportTypeMapper;
    private final AuditLogService auditLogService;
    public SupportTypeService(SupportTypeRepository supportTypeRepository, SupportTypeMapper supportTypeMapper, AuditLogService auditLogService) {
        this.supportTypeRepository = supportTypeRepository;
        this.supportTypeMapper = supportTypeMapper;
        this.auditLogService = auditLogService;
    }


    @Transactional(readOnly = true)
    public List<SupportTypeResponse> findAll() {
        return supportTypeRepository.findAll().stream()
                .map(supportTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupportTypeResponse findById(Long id) {
        return supportTypeMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTypeResponse> search(SearchRequest<SupportTypeCriteria> request) {
        SupportTypeCriteria criteria = request.getFilters();
        Pageable pageable = PageableUtils.toPageable(request.getPageable(), "supportTypeId", SORT_FIELDS);
        Specification<SupportType> spec = SupportTypeSpecification.fromCriteria(criteria);
        Page<SupportTypeResponse> page = supportTypeRepository.findAll(spec, pageable)
                .map(supportTypeMapper::toResponse);
        return PageResponse.from(page);
    }

    public SupportTypeResponse create(SupportTypeRequest request) {
        if (supportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Support type code already exists");
        }
        SupportType entity = supportTypeRepository.save(supportTypeMapper.toEntity(request));
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.SUPPORT_TYPES)
                .entityName("SupportType")
                .entityId(String.valueOf(entity.getSupportTypeId()))
                .newValue(snapshot(entity))
                .description("Création du type de support " + entity.getCode())
                .build());
        return supportTypeMapper.toResponse(entity);
    }

    public SupportTypeResponse update(Long id, SupportTypeRequest request) {
        SupportType entity = getEntity(id);
        String oldValue = snapshot(entity);
        if (!entity.getCode().equals(request.getCode())
                && supportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Support type code already exists");
        }
        supportTypeMapper.updateEntity(entity, request);
        entity = supportTypeRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.SUPPORT_TYPES)
                .entityName("SupportType")
                .entityId(String.valueOf(entity.getSupportTypeId()))
                .oldValue(oldValue)
                .newValue(snapshot(entity))
                .description("Modification du type de support " + entity.getCode())
                .build());
        return supportTypeMapper.toResponse(entity);
    }

    public void delete(Long id) {
        SupportType entity = getEntity(id);
        String oldValue = snapshot(entity);
        supportTypeRepository.delete(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.SUPPORT_TYPES)
                .entityName("SupportType")
                .entityId(String.valueOf(id))
                .oldValue(oldValue)
                .description("Suppression du type de support " + entity.getCode())
                .build());
    }

    SupportType getEntity(Long id) {
        return supportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", id));
    }

    private static String snapshot(SupportType entity) {
        return "code=" + entity.getCode() + ";label=" + entity.getLabel();
    }
}
