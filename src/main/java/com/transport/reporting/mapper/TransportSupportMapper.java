package com.transport.reporting.mapper;

import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.entity.TransportSupport;
import org.springframework.stereotype.Component;

/**
 * Mapper Support de transport : conversion Entity <-> DTO.
 */
@Component
public class TransportSupportMapper {

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
