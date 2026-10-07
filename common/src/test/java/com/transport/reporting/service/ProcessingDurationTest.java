package com.transport.reporting.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessingDurationTest {

    @Test
    void hoursAndMinutesWithinTheSameDay() {
        Instant created = Instant.parse("2026-10-07T09:00:00Z");
        Instant closed = created.plus(5, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        assertEquals(5 * 3600 + 30 * 60, PublicTrackingService.processingDurationSeconds(created, closed));
    }

    @Test
    void severalDays() {
        Instant created = Instant.parse("2026-10-01T10:00:00Z");
        Instant closed = Instant.parse("2026-10-05T16:30:00Z");
        assertEquals(4 * 86400 + 6 * 3600 + 30 * 60, PublicTrackingService.processingDurationSeconds(created, closed));
    }

    @Test
    void openReportHasNoDuration() {
        assertNull(PublicTrackingService.processingDurationSeconds(Instant.parse("2026-10-01T10:00:00Z"), null));
    }

    @Test
    void missingCreationOrInvertedDatesAreUnavailable() {
        Instant created = Instant.parse("2026-10-05T16:30:00Z");
        Instant closed = Instant.parse("2026-10-01T10:00:00Z");
        assertNull(PublicTrackingService.processingDurationSeconds(null, closed));
        assertNull(PublicTrackingService.processingDurationSeconds(created, closed));
    }
}
