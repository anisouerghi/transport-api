package com.transport.reporting.service;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportMapper;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
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
    private final ReportMapper reportMapper;

    public ReportResponse create(ReportRequest request) {
        TransportSupport support = transportSupportRepository.findByUuid(request.getSupportUuid())
                .filter(s -> s.getSupportStatus() == SupportStatus.ACTIVE)
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

        return reportMapper.toResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public ReportResponse findByReference(String reference) {
        return reportMapper.toResponse(reportRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reference)));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll() {
        return reportRepository.findAll().stream().map(reportMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse findById(Long id) {
        return reportMapper.toResponse(reportRepository.findById(id)
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
}
