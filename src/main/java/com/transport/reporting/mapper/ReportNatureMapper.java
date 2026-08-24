package com.transport.reporting.mapper;

import com.transport.reporting.dto.ReportNatureRequest;
import com.transport.reporting.dto.ReportNatureResponse;
import com.transport.reporting.entity.ReportNature;
import org.springframework.stereotype.Component;

@Component
public class ReportNatureMapper {

    public ReportNature toEntity(ReportNatureRequest request) {
        return ReportNature.builder()
                .code(normalizeCode(request.getCode()))
                .label(request.getLabel().trim())
                .description(trimToNull(request.getDescription()))
                .active(true)
                .build();
    }

    public void updateEntity(ReportNature entity, ReportNatureRequest request) {
        entity.setCode(normalizeCode(request.getCode()));
        entity.setLabel(request.getLabel().trim());
        entity.setDescription(trimToNull(request.getDescription()));
    }

    public ReportNatureResponse toResponse(ReportNature entity) {
        return ReportNatureResponse.builder()
                .reportNatureId(entity.getReportNatureId())
                .code(entity.getCode())
                .label(entity.getLabel())
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
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
