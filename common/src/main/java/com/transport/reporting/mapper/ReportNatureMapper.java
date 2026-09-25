package com.transport.reporting.mapper;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.ReportNatureRequest;
import com.transport.reporting.dto.ReportNatureResponse;
import com.transport.reporting.entity.ReportNature;
import org.springframework.stereotype.Component;

@Component
public class ReportNatureMapper {

    public ReportNature toEntity(ReportNatureRequest request) {
        ReportNature entity = ReportNature.builder()
                .code(normalizeCode(request.getCode()))
                .description(trimToNull(request.getDescription()))
                .active(true)
                .build();
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        return entity;
    }

    public void updateEntity(ReportNature entity, ReportNatureRequest request) {
        entity.setCode(normalizeCode(request.getCode()));
        LocalizedLabels.applyFrench(entity, request.getLabel());
        entity.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        entity.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        entity.setDescription(trimToNull(request.getDescription()));
    }

    public ReportNatureResponse toResponse(ReportNature entity) {
        String fr = LocalizedLabels.frOf(entity.getLabelFr(), entity.getLabel());
        return ReportNatureResponse.builder()
                .reportNatureId(entity.getReportNatureId())
                .code(entity.getCode())
                .label(LocalizedLabels.of(entity))
                .labelFr(fr)
                .labelAr(entity.getLabelAr())
                .labelEn(entity.getLabelEn())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase().replace(' ', '_');
    }

    private static String trimToNull(String value) {
        return LocalizedLabels.trimToNull(value);
    }
}
