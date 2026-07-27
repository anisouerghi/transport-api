package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.ReportTypeCriteria;
import com.transport.reporting.dto.ReportTypeRequest;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportTypeMapper;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.specification.ReportTypeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service metier ReportType (CRUD + recherche paginee + activation).
 */
@Service
@RequiredArgsConstructor
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

    @Transactional(readOnly = true)
    public List<ReportTypeResponse> findAll() {
        return reportTypeRepository.findAll().stream()
                .map(reportTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportTypeResponse> findAllActive() {
        ReportTypeCriteria criteria = new ReportTypeCriteria();
        criteria.setActive(true);
        return reportTypeRepository.findAll(ReportTypeSpecification.fromCriteria(criteria)).stream()
                .map(reportTypeMapper::toResponse)
                .toList();
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
        ReportType entity = reportTypeMapper.toEntity(request);
        return reportTypeMapper.toResponse(reportTypeRepository.save(entity));
    }

    public ReportTypeResponse update(Long id, ReportTypeRequest request) {
        ReportType entity = getEntity(id);
        if (!entity.getCode().equals(request.getCode())
                && reportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Report type code already exists");
        }
        reportTypeMapper.updateEntity(entity, request);
        return reportTypeMapper.toResponse(reportTypeRepository.save(entity));
    }

    public ReportTypeResponse setActive(Long id, boolean active) {
        ReportType entity = getEntity(id);
        entity.setActive(active);
        return reportTypeMapper.toResponse(reportTypeRepository.save(entity));
    }

    public void delete(Long id) {
        if (!reportTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReportType", id);
        }
        reportTypeRepository.deleteById(id);
    }

    ReportType getEntity(Long id) {
        return reportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportType", id));
    }
}
