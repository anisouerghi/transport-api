package com.transport.reporting.modules.support.service;

import com.transport.reporting.common.exception.BusinessException;
import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.support.dto.SupportRequest;
import com.transport.reporting.modules.support.dto.SupportResponse;
import com.transport.reporting.modules.support.entity.Support;
import com.transport.reporting.modules.support.mapper.SupportMapper;
import com.transport.reporting.modules.support.repository.SupportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportServiceImpl implements SupportService {

    private final SupportRepository supportRepository;
    private final SupportMapper supportMapper;

    @Override
    public SupportResponse create(SupportRequest request) {
        if (supportRepository.existsByReference(request.getReference())) {
            throw new BusinessException("La référence support existe déjà");
        }
        Support support = supportMapper.toEntity(request);
        return supportMapper.toResponse(supportRepository.save(support));
    }

    @Override
    public SupportResponse update(Long id, SupportRequest request) {
        Support support = findEntity(id);
        if (!support.getReference().equals(request.getReference())
                && supportRepository.existsByReference(request.getReference())) {
            throw new BusinessException("La référence support existe déjà");
        }
        supportMapper.updateEntity(support, request);
        return supportMapper.toResponse(supportRepository.save(support));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportResponse getById(Long id) {
        return supportMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportResponse getByUuid(UUID uuid) {
        Support support = supportRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Support", uuid));
        return supportMapper.toResponse(support);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportResponse getActiveByUuid(UUID uuid) {
        Support support = supportRepository.findByUuidAndActifTrue(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Support actif", uuid));
        return supportMapper.toResponse(support);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportResponse> findAll(Pageable pageable) {
        return supportRepository.findAll(pageable).map(supportMapper::toResponse);
    }

    @Override
    public SupportResponse activate(Long id) {
        Support support = findEntity(id);
        support.setActif(true);
        return supportMapper.toResponse(supportRepository.save(support));
    }

    @Override
    public SupportResponse deactivate(Long id) {
        Support support = findEntity(id);
        support.setActif(false);
        return supportMapper.toResponse(supportRepository.save(support));
    }

    private Support findEntity(Long id) {
        return supportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support", id));
    }
}
