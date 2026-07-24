package com.transport.reporting.service;

import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.dto.TransportSupportRequest;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.TransportSupportMapper;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service metier Support de transport.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SupportService {

    private final TransportSupportRepository transportSupportRepository;
    private final SupportTypeRepository supportTypeRepository;
    private final TransportSupportMapper transportSupportMapper;

    @Transactional(readOnly = true)
    public TransportSupportResponse findActiveByUuid(UUID uuid) {
        TransportSupport support = transportSupportRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("TransportSupport", uuid));
        if (support.getSupportStatus() != SupportStatus.ACTIVE) {
            throw new ResourceNotFoundException("Active TransportSupport", uuid);
        }
        return transportSupportMapper.toResponse(support);
    }

    @Transactional(readOnly = true)
    public List<TransportSupportResponse> findAll() {
        return transportSupportRepository.findAll().stream()
                .map(transportSupportMapper::toResponse)
                .toList();
    }

    public TransportSupportResponse create(TransportSupportRequest request) {
        if (transportSupportRepository.existsByReference(request.getReference())) {
            throw new BusinessException("Support reference already exists");
        }
        SupportType type = supportTypeRepository.findById(request.getSupportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", request.getSupportTypeId()));

        TransportSupport support = TransportSupport.builder()
                .reference(request.getReference())
                .label(request.getLabel())
                .qrCodeUrl(request.getQrCodeUrl())
                .qrStatus(request.getQrStatus())
                .supportStatus(request.getSupportStatus() != null ? request.getSupportStatus() : SupportStatus.ACTIVE)
                .supportType(type)
                .build();

        return transportSupportMapper.toResponse(transportSupportRepository.save(support));
    }
}
