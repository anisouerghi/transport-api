package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.DistrictCriteria;
import com.transport.reporting.dto.DistrictRequest;
import com.transport.reporting.dto.DistrictResponse;
import com.transport.reporting.entity.District;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.DistrictMapper;
import com.transport.reporting.repository.DistrictRepository;
import com.transport.reporting.specification.DistrictSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service metier District (CRUD + recherche paginee serveur).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DistrictService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "districtId",
            "codeDistrict", "codeDistrict",
            "libelleDistrict", "libelleDistrict",
            "etat", "etat"
    );

    private final DistrictRepository districtRepository;
    private final DistrictMapper districtMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<DistrictResponse> findAll() {
        return districtRepository.findAll().stream()
                .map(districtMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DistrictResponse findById(Long id) {
        return districtMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistrictResponse> search(SearchRequest<DistrictCriteria> request) {
        DistrictCriteria criteria = request.getFilters();
        Pageable pageable = PageableUtils.toPageable(request.getPageable(), "districtId", SORT_FIELDS);
        Specification<District> spec = DistrictSpecification.fromCriteria(criteria);
        Page<DistrictResponse> page = districtRepository.findAll(spec, pageable)
                .map(districtMapper::toResponse);
        return PageResponse.from(page);
    }

    public DistrictResponse create(DistrictRequest request) {
        if (districtRepository.existsByCodeDistrict(request.getCodeDistrict())) {
            throw new BusinessException("District code already exists");
        }
        District entity = districtRepository.save(districtMapper.toEntity(request));
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.DISTRICTS)
                .entityName("District")
                .entityId(String.valueOf(entity.getDistrictId()))
                .newValue(snapshot(entity))
                .description("Création du district " + entity.getCodeDistrict())
                .build());
        return districtMapper.toResponse(entity);
    }

    public DistrictResponse update(Long id, DistrictRequest request) {
        District entity = getEntity(id);
        String oldValue = snapshot(entity);
        if (!entity.getCodeDistrict().equals(request.getCodeDistrict())
                && districtRepository.existsByCodeDistrict(request.getCodeDistrict())) {
            throw new BusinessException("District code already exists");
        }
        districtMapper.updateEntity(entity, request);
        entity = districtRepository.save(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.DISTRICTS)
                .entityName("District")
                .entityId(String.valueOf(entity.getDistrictId()))
                .oldValue(oldValue)
                .newValue(snapshot(entity))
                .description("Modification du district " + entity.getCodeDistrict())
                .build());
        return districtMapper.toResponse(entity);
    }

    public void delete(Long id) {
        District entity = getEntity(id);
        String oldValue = snapshot(entity);
        districtRepository.delete(entity);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.DISTRICTS)
                .entityName("District")
                .entityId(String.valueOf(id))
                .oldValue(oldValue)
                .description("Suppression du district " + entity.getCodeDistrict())
                .build());
    }

    District getEntity(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", id));
    }

    private static String snapshot(District entity) {
        return "codeDistrict=" + entity.getCodeDistrict() + ";libelleDistrict=" + entity.getLibelleDistrict() + ";etat=" + entity.getEtat();
    }
}
