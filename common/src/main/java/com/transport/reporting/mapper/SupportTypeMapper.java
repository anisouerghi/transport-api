package com.transport.reporting.mapper;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.SupportTypeRequest;
import com.transport.reporting.dto.SupportTypeResponse;
import com.transport.reporting.entity.SupportType;
import org.springframework.stereotype.Component;

/**
 * Mapper SupportType : conversion Entity &lt;-&gt; DTO.
 */
@Component
public class SupportTypeMapper {

    public SupportType toEntity(SupportTypeRequest request) {
        SupportType entity = SupportType.builder()
                .code(request.getCode())
                .build();
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        return entity;
    }

    public void updateEntity(SupportType entity, SupportTypeRequest request) {
        entity.setCode(request.getCode());
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
    }

    public SupportTypeResponse toResponse(SupportType entity) {
        String fr = LocalizedLabels.frOf(entity.getLabelFr(), entity.getLabel());
        return SupportTypeResponse.builder()
                .supportTypeId(entity.getSupportTypeId())
                .code(entity.getCode())
                .label(LocalizedLabels.of(entity))
                .labelFr(fr)
                .labelAr(entity.getLabelAr())
                .labelEn(entity.getLabelEn())
                .build();
    }
}
