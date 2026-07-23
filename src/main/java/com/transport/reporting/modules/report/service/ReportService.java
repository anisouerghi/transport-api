package com.transport.reporting.modules.report.service;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.passenger.entity.Passenger;
import com.transport.reporting.modules.passenger.service.PassengerService;
import com.transport.reporting.modules.report.dto.ReportRequest;
import com.transport.reporting.modules.report.dto.ReportResponse;
import com.transport.reporting.modules.report.entity.Report;
import com.transport.reporting.modules.report.entity.ReportType;
import com.transport.reporting.modules.report.repository.ReportRepository;
import com.transport.reporting.modules.report.repository.ReportTypeRepository;
import com.transport.reporting.modules.status.entity.Status;
import com.transport.reporting.modules.status.service.StatusService;
import com.transport.reporting.modules.support.entity.TransportSupport;
import com.transport.reporting.modules.support.repository.TransportSupportRepository;
import com.transport.reporting.modules.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReportRepository reportRepository;
    private final ReportTypeRepository reportTypeRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final PassengerService passengerService;
    private final StatusService statusService;
    private final SupportService supportService;

    public ReportResponse create(ReportRequest request) {
        TransportSupport support = transportSupportRepository.findByUuid(request.getSupportUuid())
                .filter(s -> s.getSupportStatus() == com.transport.reporting.common.enums.SupportStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active TransportSupport", request.getSupportUuid()));

        ReportType reportType = reportTypeRepository.findById(request.getReportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ReportType", request.getReportTypeId()));

        Passenger passenger = passengerService.findOrCreate(request.getPassenger());
        Status initialStatus = statusService.findByCode("NEW");

        Report report = Report.builder()
                .reference(generateReference())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .transportSupport(support)
                .reportType(reportType)
                .passenger(passenger)
                .status(initialStatus)
                .build();

        return toResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public ReportResponse findByReference(String reference) {
        return toResponse(reportRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reference)));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll() {
        return reportRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse findById(Long id) {
        return toResponse(reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id)));
    }

    private String generateReference() {
        String reference;
        do {
            int random = ThreadLocalRandom.current().nextInt(100000, 999999);
            reference = "SIG-" + LocalDate.now().format(DATE_FORMAT) + "-" + random;
        } while (reportRepository.existsByReference(reference));
        return reference;
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .uuid(report.getUuid())
                .reference(report.getReference())
                .creationDate(report.getCreationDate())
                .description(report.getDescription())
                .priority(report.getPriority())
                .closureDate(report.getClosureDate())
                .transportSupport(supportService.toResponse(report.getTransportSupport()))
                .reportTypeCode(report.getReportType().getCode())
                .reportTypeLabel(report.getReportType().getLabel())
                .passenger(passengerService.toResponse(report.getPassenger()))
                .status(statusService.toResponse(report.getStatus()))
                .build();
    }
}
