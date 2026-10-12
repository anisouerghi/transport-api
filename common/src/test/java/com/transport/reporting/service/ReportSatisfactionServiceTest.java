package com.transport.reporting.service;

import com.transport.reporting.common.enums.SatisfactionLevel;
import com.transport.reporting.dto.PublicSatisfactionRequest;
import com.transport.reporting.dto.PublicSatisfactionResponse;
import com.transport.reporting.dto.SatisfactionSummaryResponse;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportSatisfaction;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportSatisfactionRepository;
import com.transport.reporting.security.PassengerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSatisfactionServiceTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ReportSatisfactionRepository satisfactionRepository = mock(ReportSatisfactionRepository.class);
    private final ReportSatisfactionService service =
            new ReportSatisfactionService(reportRepository, satisfactionRepository);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void closedAnonymousReportAcceptsEachLevelWithoutComment() {
        Report report = closedReport(null);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(satisfactionRepository.existsByReport_ReportId(1L)).thenReturn(false);
        when(satisfactionRepository.save(any(ReportSatisfaction.class))).thenAnswer(invocation -> {
            ReportSatisfaction saved = invocation.getArgument(0);
            saved.setSatisfactionId(9L);
            saved.prePersist();
            return saved;
        });

        for (SatisfactionLevel level : SatisfactionLevel.values()) {
            PublicSatisfactionRequest request = new PublicSatisfactionRequest();
            request.setLevel(level);
            PublicSatisfactionResponse response = service.submit(report.getUuid(), request);
            assertEquals(level.name(), response.getLevel());
            assertNull(response.getComment());
        }
    }

    @Test
    void commentIsOptionalAndMarkupIsRemoved() {
        assertNull(ReportSatisfactionService.sanitizeComment("   "));
        assertEquals("alert(1)", ReportSatisfactionService.sanitizeComment("<script>alert(1)</script>"));
        assertEquals("utile", ReportSatisfactionService.sanitizeComment(" <b>utile</b> "));
    }

    @Test
    void openReportIsRefused() {
        Report report = closedReport(null);
        report.setStatus(status("IN_PROGRESS"));
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));

        PublicSatisfactionRequest request = request(SatisfactionLevel.SATISFAIT);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(report.getUuid(), request));
        assertEquals("SATISFACTION_NOT_CLOSED", ex.getErrorCode());
        verify(satisfactionRepository, never()).save(any());
    }

    @Test
    void anotherPassengerCannotRateATrackedReport() {
        Passenger owner = trackedPassenger(4L);
        Report report = closedReport(owner);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        authenticate(8L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(report.getUuid(), request(SatisfactionLevel.NON_SATISFAIT)));
        assertEquals("SATISFACTION_FORBIDDEN", ex.getErrorCode());
    }

    @Test
    void trackedOwnerCanRate() {
        Passenger owner = trackedPassenger(4L);
        Report report = closedReport(owner);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(satisfactionRepository.existsByReport_ReportId(1L)).thenReturn(false);
        when(satisfactionRepository.save(any(ReportSatisfaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        authenticate(4L);

        PublicSatisfactionResponse response = service.submit(report.getUuid(), request(SatisfactionLevel.MOYENNEMENT_SATISFAIT));
        assertEquals("MOYENNEMENT_SATISFAIT", response.getLevel());
    }

    @Test
    void trackedReportWithoutLoginIsRefused() {
        Report report = closedReport(trackedPassenger(4L));
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(report.getUuid(), request(SatisfactionLevel.SATISFAIT)));
        assertEquals("AUTH_REQUIRED", ex.getErrorCode());
    }

    @Test
    void secondEvaluationIsRefused() {
        Report report = closedReport(null);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(satisfactionRepository.existsByReport_ReportId(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(report.getUuid(), request(SatisfactionLevel.SATISFAIT)));
        assertEquals("SATISFACTION_ALREADY_EXISTS", ex.getErrorCode());
        verify(satisfactionRepository, never()).save(any());
    }

    @Test
    void concurrentInsertIsRefused() {
        Report report = closedReport(null);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(satisfactionRepository.existsByReport_ReportId(1L)).thenReturn(false);
        when(satisfactionRepository.save(any(ReportSatisfaction.class)))
                .thenThrow(new DataIntegrityViolationException("uk_report_satisfaction_report"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(report.getUuid(), request(SatisfactionLevel.SATISFAIT)));
        assertEquals("SATISFACTION_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    void summaryUsesEvaluationsForLevelsAndClosedReportsForParticipation() {
        when(reportRepository.countClosedEligible(null, null, null, null)).thenReturn(4L);
        when(satisfactionRepository.countByLevel(null, null, null, null)).thenReturn(List.of(
                new Object[]{SatisfactionLevel.SATISFAIT, 2L},
                new Object[]{SatisfactionLevel.NON_SATISFAIT, 1L}));

        SatisfactionSummaryResponse summary = service.summarize(null, null, null, null);
        assertEquals(4L, summary.getEligibleClosedReports());
        assertEquals(3L, summary.getEvaluationsReceived());
        assertEquals(75d, summary.getParticipationRate());
        assertEquals(66.7d, summary.getSatisfiedRate());
        assertEquals(0d, summary.getPartialRate());
        assertEquals(33.3d, summary.getDissatisfiedRate());
        assertTrue(service.canSubmit(closedReport(null), null));
    }

    private static PublicSatisfactionRequest request(SatisfactionLevel level) {
        PublicSatisfactionRequest request = new PublicSatisfactionRequest();
        request.setLevel(level);
        return request;
    }

    private static Report closedReport(Passenger passenger) {
        Report report = new Report();
        report.setReportId(1L);
        report.setUuid(UUID.randomUUID());
        report.setStatus(status("CLOSED"));
        report.setPassenger(passenger);
        return report;
    }

    private static Status status(String code) {
        Status status = new Status();
        status.setCode(code);
        return status;
    }

    private static Passenger trackedPassenger(Long id) {
        Passenger passenger = new Passenger();
        passenger.setPassengerId(id);
        passenger.setPasswordHash("hash");
        return passenger;
    }

    private static void authenticate(Long passengerId) {
        PassengerPrincipal principal = new PassengerPrincipal(passengerId, "a@b.c", "Nom", null, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
