package com.transport.reporting.modules.support.service;

import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.common.exception.BusinessException;
import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.support.dto.TransportSupportRequest;
import com.transport.reporting.modules.support.dto.TransportSupportResponse;
import com.transport.reporting.modules.support.entity.SupportType;
import com.transport.reporting.modules.support.entity.TransportSupport;
import com.transport.reporting.modules.support.repository.SupportTypeRepository;
import com.transport.reporting.modules.support.repository.TransportSupportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportService {

    private final TransportSupportRepository transportSupportRepository;
    private final SupportTypeRepository supportTypeRepository;

    @Transactional(readOnly = true)
    public TransportSupportResponse findActiveByUuid(UUID uuid) {
        TransportSupport support = transportSupportRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("TransportSupport", uuid));
        if (support.getSupportStatus() != SupportStatus.ACTIVE) {
            throw new ResourceNotFoundException("Active TransportSupport", uuid);
        }
        return toResponse(support);
    }

    @Transactional(readOnly = true)
    public List<TransportSupportResponse> findAll() {
        return transportSupportRepository.findAll().stream().map(this::toResponse).toList();
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

        return toResponse(transportSupportRepository.save(support));
    }

    public TransportSupportResponse toResponse(TransportSupport support) {
        return TransportSupportResponse.builder()
                .transportSupportId(support.getTransportSupportId())
                .uuid(support.getUuid())
                .reference(support.getReference())
                .label(support.getLabel())
                .qrCodeUrl(support.getQrCodeUrl())
                .qrDateCreation(support.getQrDateCreation())
                .qrDateImpression(support.getQrDateImpression())
                .qrStatus(support.getQrStatus())
                .supportStatus(support.getSupportStatus())
                .supportTypeCode(support.getSupportType().getCode())
                .supportTypeLabel(support.getSupportType().getLabel())
                .build();
    }
}
