package com.transport.reporting.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {
    }

    public static Instant now() {
        return Instant.now();
    }

    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DISPLAY_FORMAT.format(instant.atZone(DEFAULT_ZONE));
    }
}
