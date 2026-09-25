package com.transport.reporting.mapper;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.Status;
import org.springframework.stereotype.Component;

/**
 * Mapper Signalement : conversion Entity &lt;-&gt; DTO.
 */
@Component
public class ReportMapper {

    private final TransportSupportMapper transportSupportMapper;
    private final PassengerMapper passengerMapper;
    private final StatusMapper statusMapper;

    public ReportMapper(
            TransportSupportMapper transportSupportMapper,
            PassengerMapper passengerMapper,
            StatusMapper statusMapper) {
        this.transportSupportMapper = transportSupportMapper;
        this.passengerMapper = passengerMapper;
        this.statusMapper = statusMapper;
    }

    public ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .description(report.getDescription())
                .priority(report.getPriority())
                .closureDate(report.getClosureDate())
                .publish(report.getPublish())
                .publishDate(report.getPublishDate())
                .sendEmail(report.getSendEmail())
                .sendEmailDate(report.getSendEmailDate())
                .publicResponse(report.getPublicResponse())
                .publicResponseDate(report.getPublicResponseDate())
                .transportSupport(report.getTransportSupport() == null
                    ? null
                    : transportSupportMapper.toResponse(report.getTransportSupport()))
                .reportTypeCode(report.getReportType().getCode())
                .reportTypeLabel(LocalizedLabels.of(report.getReportType()))
                .natureId(report.getNature() != null ? report.getNature().getReportNatureId() : null)
                .natureCode(report.getNature() != null ? report.getNature().getCode() : null)
                .natureLabel(report.getNature() != null ? LocalizedLabels.of(report.getNature()) : null)
                .passenger(passengerMapper.toResponse(report.getPassenger()))
                .status(toStatusResponse(report.getStatus()))
                .build();
    }

    public StatusResponse toStatusResponse(Status status) {
        return statusMapper.toResponse(status);
    }
}
