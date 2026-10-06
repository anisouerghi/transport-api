package com.transport.reporting.service;

import com.transport.reporting.common.enums.ReplyAuthorType;
import com.transport.reporting.common.enums.ReplyType;
import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.dto.ReplyCreateResult;
import com.transport.reporting.dto.ReplyRequest;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Reply;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.mapper.ReplyMapper;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReplyRepository;
import com.transport.reporting.repository.ReportHistoryRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.security.PassengerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplyConversationTest {

    private final ReplyRepository replyRepository = mock(ReplyRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ReportHistoryRepository reportHistoryRepository = mock(ReportHistoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);
    private final StatusRepository statusRepository = mock(StatusRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private ReplyService replyService;
    private PublicTrackingService trackingService;

    @BeforeEach
    void setUp() {
        replyService = new ReplyService(
                replyRepository,
                reportRepository,
                reportHistoryRepository,
                userRepository,
                passengerRepository,
                statusRepository,
                new ReplyMapper(),
                auditLogService,
                mock(com.transport.reporting.security.PermissionChecker.class),
                mock(EmailService.class),
                mock(ReplyEmailComposer.class));
        trackingService = new PublicTrackingService(reportRepository, replyRepository);
        when(replyRepository.save(any(Reply.class))).thenAnswer(invocation -> {
            Reply reply = invocation.getArgument(0);
            if (reply.getReplyId() == null) {
                reply.setReplyId(50L);
            }
            if (reply.getReplyDate() == null) {
                reply.setReplyDate(Instant.parse("2026-02-01T10:00:00Z"));
            }
            return reply;
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agentResponseDefaultsToResponseAndAgent() {
        Report report = openReport();
        when(reportRepository.findByIdWithPassenger(1L)).thenReturn(Optional.of(report));
        when(userRepository.findById(7L)).thenReturn(Optional.of(agent()));

        ReplyCreateResult result = replyService.create(1L, request(null));

        assertEquals("RESPONSE", result.getReply().getReplyType());
        assertEquals("AGENT", result.getReply().getAuthorType());
        assertTrue(result.getReply().isPublicResponse());
    }

    @Test
    void agentComplementRequestOpensStatusAndStaysPublic() {
        Report report = openReport();
        Status waiting = status("DEMANDE_COMPLEMENT", 4L);
        when(reportRepository.findByIdWithPassenger(1L)).thenReturn(Optional.of(report));
        when(userRepository.findById(7L)).thenReturn(Optional.of(agent()));
        when(statusRepository.findByCode("DEMANDE_COMPLEMENT")).thenReturn(Optional.of(waiting));
        when(statusRepository.findById(4L)).thenReturn(Optional.of(waiting));

        ReplyRequest request = request(ReplyType.COMPLEMENT_REQUEST);
        request.setPublicResponse(false);
        ReplyCreateResult result = replyService.create(1L, request);

        assertEquals("COMPLEMENT_REQUEST", result.getReply().getReplyType());
        assertEquals("AGENT", result.getReply().getAuthorType());
        assertTrue(result.getReply().isPublicResponse());
        assertEquals("DEMANDE_COMPLEMENT", report.getStatus().getCode());
        verify(reportHistoryRepository).save(any());
    }

    @Test
    void passengerComplementResponseUsesTokenIdentityAndResumesProcessing() {
        Report report = openReport();
        report.setUuid(UUID.randomUUID());
        report.setPassenger(passenger(5L));
        report.setStatus(status("DEMANDE_COMPLEMENT", 4L));
        Status inProgress = status("IN_PROGRESS", 2L);
        authenticate(5L);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(replyRepository.findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(1L))
                .thenReturn(List.of(message(1L, ReplyType.COMPLEMENT_REQUEST, true)));
        when(passengerRepository.findById(5L)).thenReturn(Optional.of(passenger(5L)));
        when(statusRepository.findByCode("IN_PROGRESS")).thenReturn(Optional.of(inProgress));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgress));

        ReplyResponse response = replyService.addPassengerComplement(report.getUuid(), "Voici le complément.");

        assertEquals("COMPLEMENT_RESPONSE", response.getReplyType());
        assertEquals("PASSENGER", response.getAuthorType());
        assertEquals(5L, response.getPassengerId());
        assertEquals("IN_PROGRESS", report.getStatus().getCode());
        verify(reportHistoryRepository).save(any());
    }

    @Test
    void passengerReplyRefusedWhenNoComplementIsPending() {
        Report report = ownedOpenReport();
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(replyRepository.findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(1L))
                .thenReturn(List.of(message(1L, ReplyType.RESPONSE, true)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> replyService.addPassengerComplement(report.getUuid(), "trop tôt"));

        assertEquals("COMPLEMENT_NOT_PENDING", ex.getErrorCode());
        verify(replyRepository, never()).save(any());
    }

    @Test
    void passengerReplyRefusedWhenReportIsClosed() {
        Report byStatus = ownedOpenReport();
        byStatus.setStatus(status("CLOSED", 9L));
        when(reportRepository.findByUuid(byStatus.getUuid())).thenReturn(Optional.of(byStatus));
        BusinessException closed = assertThrows(BusinessException.class,
                () -> replyService.addPassengerComplement(byStatus.getUuid(), "trop tard"));
        assertEquals("REPORT_CLOSED", closed.getErrorCode());

        Report byDate = ownedOpenReport();
        byDate.setClosureDate(Instant.parse("2026-01-02T00:00:00Z"));
        when(reportRepository.findByUuid(byDate.getUuid())).thenReturn(Optional.of(byDate));
        BusinessException dated = assertThrows(BusinessException.class,
                () -> replyService.addPassengerComplement(byDate.getUuid(), "trop tard"));
        assertEquals("REPORT_CLOSED", dated.getErrorCode());
        verify(replyRepository, never()).save(any());
    }

    @Test
    void passengerReplyRefusedForAnotherPassenger() {
        Report report = ownedOpenReport();
        report.setPassenger(passenger(9L));
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> replyService.addPassengerComplement(report.getUuid(), "pas à moi"));

        assertEquals("REPLY_FORBIDDEN", ex.getErrorCode());
        verify(replyRepository, never()).save(any());
    }

    @Test
    void internalNoteIsVisibleToAdminAndHiddenFromPublicTracking() {
        Report report = openReport();
        report.setUuid(UUID.randomUUID());
        when(reportRepository.findByIdWithPassenger(1L)).thenReturn(Optional.of(report));
        when(userRepository.findById(7L)).thenReturn(Optional.of(agent()));
        ReplyCreateResult created = replyService.create(1L, request(ReplyType.INTERNAL_NOTE));
        assertEquals("INTERNAL_NOTE", created.getReply().getReplyType());
        assertFalse(created.getReply().isPublicResponse());

        Reply note = message(1L, ReplyType.INTERNAL_NOTE, true);
        note.setMessage("secret");
        note.setReport(report);
        Reply visible = message(2L, ReplyType.COMPLEMENT_REQUEST, true);
        visible.setMessage("merci de préciser");
        visible.setReport(report);
        when(reportRepository.findByUuid(report.getUuid())).thenReturn(Optional.of(report));
        when(replyRepository.findByReport_ReportIdAndPublicResponseTrueAndReplyTypeNotOrderByReplyDateAscReplyIdAsc(
                eq(1L), eq(ReplyType.INTERNAL_NOTE)))
                .thenReturn(List.of(note, visible));
        when(reportRepository.existsById(1L)).thenReturn(true);
        when(replyRepository.findByReport_ReportIdOrderByReplyDateAscReplyIdAsc(1L))
                .thenReturn(List.of(note, visible));

        List<ReplyResponse> admin = replyService.findByReportId(1L);
        assertEquals(2, admin.size());
        assertEquals("INTERNAL_NOTE", admin.get(0).getReplyType());

        PublicReportTrackingResponse tracking = trackingService.findByUuid(report.getUuid());
        assertEquals(1, tracking.getReplies().size());
        assertEquals("merci de préciser", tracking.getReplies().get(0).getMessage());
        assertEquals("COMPLEMENT_REQUEST", tracking.getReplies().get(0).getReplyType());
        assertTrue(tracking.isCanPassengerReply());

        Reply secret = message(3L, ReplyType.INTERNAL_NOTE, true);
        secret.setMessage("secret accueil");
        secret.setReport(report);
        Reply hello = message(4L, ReplyType.RESPONSE, true);
        hello.setMessage("bonjour");
        hello.setReport(report);
        when(replyRepository.findPublicHomepageReplies(eq(ReplyType.INTERNAL_NOTE), any(Pageable.class)))
                .thenReturn(List.of(secret, hello));
        assertEquals(List.of("bonjour"),
                trackingService.listHomepageReplies(0, 4).getContent().stream()
                        .map(item -> item.getMessage())
                        .toList());
    }

    @Test
    void chronologyOrdersByReplyDateThenReplyId() {
        Instant same = Instant.parse("2026-01-01T00:00:00Z");
        Reply laterId = message(8L, ReplyType.RESPONSE, true);
        laterId.setReplyDate(same);
        Reply earlierId = message(3L, ReplyType.RESPONSE, true);
        earlierId.setReplyDate(same);
        Reply older = message(9L, ReplyType.RESPONSE, true);
        older.setReplyDate(same.minusSeconds(60));

        List<Reply> ordered = ReplyService.chronological(List.of(laterId, older, earlierId));

        assertEquals(List.of(9L, 3L, 8L), ordered.stream().map(Reply::getReplyId).toList());
    }

    @Test
    void legacyRowsWithoutTypeStayResponseFromAgent() {
        Reply legacy = new Reply();
        legacy.setReplyId(1L);
        legacy.setMessage("ancienne réponse");
        legacy.setReplyDate(Instant.parse("2025-01-01T00:00:00Z"));
        legacy.setPublicResponse(true);
        legacy.setReport(openReport());

        assertEquals(ReplyType.RESPONSE, legacy.effectiveType());
        assertEquals(ReplyAuthorType.AGENT, legacy.effectiveAuthor());
        ReplyResponse response = new ReplyMapper().toResponse(legacy);
        assertEquals("RESPONSE", response.getReplyType());
        assertEquals("AGENT", response.getAuthorType());

        Reply built = Reply.builder().message("nouveau").report(openReport()).build();
        assertEquals(ReplyType.RESPONSE, built.getReplyType());
        assertEquals(ReplyAuthorType.AGENT, built.getAuthorType());
    }

    @Test
    void agentCannotCreatePassengerComplementResponse() {
        when(reportRepository.findByIdWithPassenger(1L)).thenReturn(Optional.of(openReport()));
        when(userRepository.findById(7L)).thenReturn(Optional.of(agent()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> replyService.create(1L, request(ReplyType.COMPLEMENT_RESPONSE)));

        assertEquals("REPLY_TYPE_FORBIDDEN", ex.getErrorCode());
        verify(replyRepository, never()).save(any());
    }

    private Report ownedOpenReport() {
        Report report = openReport();
        report.setUuid(UUID.randomUUID());
        report.setPassenger(passenger(5L));
        authenticate(5L);
        return report;
    }

    private static Report openReport() {
        Report report = new Report();
        report.setReportId(1L);
        report.setReference("SIG-20260101-000001");
        report.setStatus(status("IN_PROGRESS", 2L));
        return report;
    }

    private static Status status(String code, long id) {
        Status status = new Status();
        status.setStatusId(id);
        status.setCode(code);
        return status;
    }

    private static AppUser agent() {
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setUsername("agent");
        user.setName("Agent");
        return user;
    }

    private static Passenger passenger(long id) {
        Passenger passenger = new Passenger();
        passenger.setPassengerId(id);
        return passenger;
    }

    private static ReplyRequest request(ReplyType type) {
        ReplyRequest request = new ReplyRequest();
        request.setMessage("message");
        request.setUserId(7L);
        request.setReplyType(type);
        return request;
    }

    private static Reply message(long id, ReplyType type, boolean publicResponse) {
        return Reply.builder()
                .replyId(id)
                .message("m" + id)
                .replyDate(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(id))
                .publicResponse(publicResponse)
                .replyType(type)
                .authorType(ReplyAuthorType.AGENT)
                .build();
    }

    private static void authenticate(long passengerId) {
        PassengerPrincipal principal = new PassengerPrincipal(passengerId, "a@b.c", "A", "1", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
