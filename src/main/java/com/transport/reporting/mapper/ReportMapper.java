package com.transport.reporting.mapper;

import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper Signalement : conversion Entity <-> DTO.
 */
@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final TransportSupportMapper transportSupportMapper;
    private final PassengerMapper passengerMapper;

    public ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .description(report.getDescription())
                .priority(report.getPriority())
                .closureDate(report.getClosureDate())
                .transportSupport(transportSupportMapper.toResponse(report.getTransportSupport()))
                .reportTypeCode(report.getReportType().getCode())
                .reportTypeLabel(report.getReportType().getLabel())
                .passenger(passengerMapper.toResponse(report.getPassenger()))
                .status(toStatusResponse(report.getStatus()))
                .build();
    }

    public StatusResponse toStatusResponse(Status status) {
        return StatusResponse.builder()
                .statusId(status.getStatusId())
                .code(status.getCode())
                .label(status.getLabel())
                .displayOrder(status.getDisplayOrder())
                .build();
    }
}
