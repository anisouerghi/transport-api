package com.transport.reporting.mapper;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.ReportTypeRequest;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.entity.ReportType;
import org.springframework.stereotype.Component;

/**
 * Mapper ReportType : conversion Entity &lt;-&gt; DTO.
 */
@Component
public class ReportTypeMapper {

    public ReportType toEntity(ReportTypeRequest request) {
        ReportType entity = ReportType.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .priority(request.getPriority() == null ? 100 : request.getPriority())
                .icon(trimIcon(request.getIcon()))
                .active(true)
                .build();
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        return entity;
    }

    public void updateEntity(ReportType entity, ReportTypeRequest request) {
        entity.setCode(request.getCode());
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        entity.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        entity.setIcon(trimIcon(request.getIcon()));
    }

    private static String trimIcon(String icon) {
        if (icon == null) {
            return null;
        }
        String value = icon.trim();
        return value.isEmpty() ? null : value;
    }

    public ReportTypeResponse toResponse(ReportType entity) {
        String fr = LocalizedLabels.frOf(entity.getLabelFr(), entity.getLabel());
        return ReportTypeResponse.builder()
                .reportTypeId(entity.getReportTypeId())
                .code(entity.getCode())
                .label(LocalizedLabels.of(entity))
                .labelFr(fr)
                .labelAr(entity.getLabelAr())
                .labelEn(entity.getLabelEn())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .icon(entity.getIcon())
                .active(entity.isActive())
                .build();
    }
}
