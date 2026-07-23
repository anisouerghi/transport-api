package com.transport.reporting.common.utils;

import com.transport.reporting.common.constants.AppConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ReferenceUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReferenceUtils() {
    }

    public static String generateSignalementReference() {
        String date = LocalDate.now().format(DATE_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return AppConstants.REFERENCE_PREFIX + "-" + date + "-" + random;
    }

    public static UUID generateUuid() {
        return UUID.randomUUID();
    }
}
