package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.SupportTypeCriteria;
import com.transport.reporting.dto.SupportTypeRequest;
import com.transport.reporting.dto.SupportTypeResponse;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.SupportTypeMapper;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.specification.SupportTypeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service metier SupportType (CRUD + recherche paginee serveur).
 * <p>
 * Pattern de recherche :
 * <ol>
 *   <li>Construire un {@link Pageable} depuis la requete</li>
 *   <li>Construire une {@link Specification} depuis les criteres</li>
 *   <li>Executer findAll(spec, pageable) puis mapper en DTO</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SupportTypeService {

    /** Mapping nom logique frontend -> attribut JPA autorise pour le tri. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "supportTypeId",
            "code", "code",
            "label", "label"
    );

    private final SupportTypeRepository supportTypeRepository;
    private final SupportTypeMapper supportTypeMapper;

    /** Liste complete (sans pagination) — utile pour les listes deroulantes. */
    @Transactional(readOnly = true)
    public List<SupportTypeResponse> findAll() {
        return supportTypeRepository.findAll().stream()
                .map(supportTypeMapper::toResponse)
                .toList();
    }

    /** Consultation par identifiant technique. */
    @Transactional(readOnly = true)
    public SupportTypeResponse findById(Long id) {
        return supportTypeMapper.toResponse(getEntity(id));
    }

    /**
     * Recherche paginee multicritere.
     *
     * @param request contient filters + pageable
     * @return page de resultats enveloppee dans PageResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<SupportTypeResponse> search(SearchRequest<SupportTypeCriteria> request) {
        SupportTypeCriteria criteria = request.getFilters();
        Pageable pageable = PageableUtils.toPageable(request.getPageable(), "supportTypeId", SORT_FIELDS);
        Specification<SupportType> spec = SupportTypeSpecification.fromCriteria(criteria);
        Page<SupportTypeResponse> page = supportTypeRepository.findAll(spec, pageable)
                .map(supportTypeMapper::toResponse);
        return PageResponse.from(page);
    }

    /** Creation : verifie l'unicite du code avant insertion. */
    public SupportTypeResponse create(SupportTypeRequest request) {
        if (supportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Support type code already exists");
        }
        SupportType entity = supportTypeMapper.toEntity(request);
        return supportTypeMapper.toResponse(supportTypeRepository.save(entity));
    }

    /** Modification : verifie l'unicite du code si celui-ci change. */
    public SupportTypeResponse update(Long id, SupportTypeRequest request) {
        SupportType entity = getEntity(id);
        if (!entity.getCode().equals(request.getCode())
                && supportTypeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Support type code already exists");
        }
        supportTypeMapper.updateEntity(entity, request);
        return supportTypeMapper.toResponse(supportTypeRepository.save(entity));
    }

    /** Suppression physique. Echoue si l'id n'existe pas. */
    public void delete(Long id) {
        SupportType entity = getEntity(id);
        supportTypeRepository.delete(entity);
    }

    /** Charge l'entite ou leve ResourceNotFoundException. */
    SupportType getEntity(Long id) {
        return supportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", id));
    }
}
