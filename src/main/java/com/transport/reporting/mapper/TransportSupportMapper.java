package com.transport.reporting.mapper;

import com.transport.reporting.dto.TransportSupportRequest;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.entity.TransportSupport;
import org.springframework.stereotype.Component;

/**
 * Mapper TransportSupport : conversion Entity &lt;-&gt; DTO.
 * Les champs QR (uuid, url, path, status) ne sont jamais mappes depuis le Request
 * — ils sont geres exclusivement par le service / @PrePersist.
 */
@Component
public class TransportSupportMapper {

    /**
     * Request -> nouvelle entite (creation).
     * UUID / QR seront renseignes ensuite par le service.
     */
    public TransportSupport toEntity(TransportSupportRequest request, SupportType supportType) {
        TransportSupport entity = new TransportSupport();
        entity.setReference(request.getReference());
        entity.setLabel(request.getLabel());
        entity.setSupportStatus(request.getSupportStatus());
        entity.setSupportType(supportType);
        return entity;
    }

    /**
     * Applique le Request sur une entite existante (modification).
     * Ne touche pas aux champs QR.
     */
    public void updateEntity(TransportSupport entity, TransportSupportRequest request, SupportType supportType) {
        entity.setReference(request.getReference());
        entity.setLabel(request.getLabel());
        if (request.getSupportStatus() != null) {
            entity.setSupportStatus(request.getSupportStatus());
        }
        entity.setSupportType(supportType);
        if (request.getVersion() != null) {
            entity.setVersion(request.getVersion());
        }
    }

    /**
     * Entite -> Response complete (inclut type denormalise + QR + audit).
     */
    public TransportSupportResponse toResponse(TransportSupport support) {
        return TransportSupportResponse.builder()
                .transportSupportId(support.getTransportSupportId())
                .uuid(support.getUuid())
                .reference(support.getReference())
                .label(support.getLabel())
                .qrCodeUrl(support.getQrCodeUrl())
                .qrCodePath(support.getQrCodePath())
                .qrDateCreation(support.getQrDateCreation())
                .qrDateImpression(support.getQrDateImpression())
                .qrStatus(support.getQrStatus())
                .supportStatus(support.getSupportStatus())
                .supportTypeId(support.getSupportType().getSupportTypeId())
                .supportTypeCode(support.getSupportType().getCode())
                .supportTypeLabel(support.getSupportType().getLabel())
                .createdAt(support.getCreatedAt())
                .updatedAt(support.getUpdatedAt())
                .version(support.getVersion())
                .build();
    }
}
