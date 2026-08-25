package com.transport.reporting.mapper;

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
        return ReportType.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .description(request.getDescription())
                .active(true)
                .build();
    }

    public void updateEntity(ReportType entity, ReportTypeRequest request) {
        entity.setCode(request.getCode());
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
    }

    public ReportTypeResponse toResponse(ReportType entity) {
        return ReportTypeResponse.builder()
                .reportTypeId(entity.getReportTypeId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .description(entity.getDescription())
                .active(entity.isActive())
                .build();
    }
}
