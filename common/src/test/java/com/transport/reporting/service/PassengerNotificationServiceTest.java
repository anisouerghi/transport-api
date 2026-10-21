package com.transport.reporting.service;

import com.transport.reporting.dto.PassengerNotificationInboxResponse;
import com.transport.reporting.dto.PassengerNotificationReadRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerNotification;
import com.transport.reporting.entity.Report;
import com.transport.reporting.repository.PassengerNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PassengerNotificationServiceTest {

    private final PassengerNotificationRepository repository = mock(PassengerNotificationRepository.class);
    private final PassengerPlatformNotificationGuard platformGuard =
            mock(PassengerPlatformNotificationGuard.class);
    private PassengerNotificationService service;

    @BeforeEach
    void setUp() {
        service = new PassengerNotificationService(repository, platformGuard, new PublicStatusMapper());
        when(platformGuard.evaluate(any(Passenger.class)))
                .thenReturn(PassengerPlatformNotificationGuard.Decision.ALLOW);
    }

    @Test
    void authenticatedPassengerReceivesStatusNotification() {
        Report report = reportWithPassenger(trackedPassenger());
        when(repository.existsByPassenger_PassengerIdAndDedupKey(eq(10L), any())).thenReturn(false);
        when(repository.save(any(PassengerNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean created = service.createForStatusOrComplement(
                report,
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED,
                "En cours de traitement",
                null);

        assertTrue(created);
        verify(repository).save(any(PassengerNotification.class));
    }

    @Test
    void anonymousPassengerReceivesNoNotification() {
        Report report = reportWithPassenger(anonymousPassenger());

        boolean created = service.createForStatusOrComplement(
                report,
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED,
                "En cours de traitement",
                null);

        assertFalse(created);
        verify(repository, never()).save(any());
    }

    @Test
    void notifyPlatformDisabledSkipsCreation() {
        when(platformGuard.evaluate(any(Passenger.class)))
                .thenReturn(PassengerPlatformNotificationGuard.Decision.DENY_DISABLED);
        Report report = reportWithPassenger(trackedPassenger());

        boolean created = service.createForStatusOrComplement(
                report,
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED,
                "Traité",
                null);

        assertFalse(created);
        verify(repository, never()).save(any());
    }

    @Test
    void duplicateDedupKeyIsIgnored() {
        Report report = reportWithPassenger(trackedPassenger());
        when(repository.existsByPassenger_PassengerIdAndDedupKey(eq(10L), any())).thenReturn(true);

        boolean created = service.createForStatusOrComplement(
                report,
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED,
                "En cours de traitement",
                null);

        assertFalse(created);
        verify(repository, never()).save(any());
    }

    @Test
    void inboxReturnsUnreadCountAndItems() {
        PassengerNotification n = PassengerNotification.builder()
                .notificationId(1L)
                .passenger(trackedPassenger())
                .report(reportWithPassenger(trackedPassenger()))
                .eventType(PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED)
                .publicStatus("Traité")
                .message("Signalement WS-1 — Traité")
                .dedupKey("STATUS:1:traite")
                .createdAt(Instant.parse("2026-04-01T10:00:00Z"))
                .build();
        when(repository.countByPassenger_PassengerIdAndReadAtIsNull(10L)).thenReturn(1L);
        when(repository.findByPassenger_PassengerIdOrderByCreatedAtDescNotificationIdDesc(
                eq(10L), any(Pageable.class)))
                .thenReturn(List.of(n));

        PassengerNotificationInboxResponse inbox = service.inbox(10L);

        assertEquals(1L, inbox.getUnreadCount());
        assertEquals(1, inbox.getItems().size());
        assertEquals("WS-ABC", inbox.getItems().get(0).getReportReference());
        assertFalse(inbox.getItems().get(0).isRead());
    }

    @Test
    void markReadScopesToCurrentPassenger() {
        when(repository.markRead(eq(10L), any(), eq(true), any())).thenReturn(3);

        int updated = service.markRead(10L, PassengerNotificationReadRequest.builder().markAll(true).build());

        assertEquals(3, updated);
        verify(repository).markRead(eq(10L), any(), eq(true), any());
    }

    @Test
    void buildDedupKeyStableForSamePublicStatus() {
        String a = PassengerNotificationService.buildDedupKey(
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED, 5L, "En cours de traitement", null);
        String b = PassengerNotificationService.buildDedupKey(
                PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED, 5L, "en cours de traitement", null);
        assertEquals(a, b);
        assertEquals("COMPLEMENT:reply:9", PassengerNotificationService.buildDedupKey(
                PassengerNotification.EVENT_COMPLEMENT_REQUEST, 5L, "Infos", 9L));
    }

    private static Passenger trackedPassenger() {
        Passenger p = new Passenger();
        p.setPassengerId(10L);
        p.setEmail("user@example.com");
        p.setPasswordHash("$2a$hashed");
        return p;
    }

    private static Passenger anonymousPassenger() {
        Passenger p = new Passenger();
        p.setPassengerId(11L);
        p.setEmail("anon@example.com");
        return p;
    }

    private static Report reportWithPassenger(Passenger passenger) {
        Report report = new Report();
        report.setReportId(1L);
        report.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        report.setReference("WS-ABC");
        report.setPassenger(passenger);
        return report;
    }
}
