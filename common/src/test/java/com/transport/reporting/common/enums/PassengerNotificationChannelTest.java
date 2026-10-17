package com.transport.reporting.common.enums;

import com.transport.reporting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassengerNotificationChannelTest {

    @Test
    void defaultIdsArePlatformAndEmail() {
        assertEquals(List.of(1, 2), PassengerNotificationChannel.defaultIds());
    }

    @Test
    void normalizeAcceptsEmptyList() {
        assertTrue(PassengerNotificationChannel.normalizeIds(List.of()).isEmpty());
    }

    @Test
    void normalizeRejectsUnknownChannel() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> PassengerNotificationChannel.normalizeIds(List.of(1, 99)));
        assertEquals(PassengerNotificationChannel.ERROR_CODE, ex.getErrorCode());
    }

    @Test
    void toIdsMapsBooleans() {
        assertEquals(
                List.of(1, 4),
                PassengerNotificationChannel.toIds(true, false, false, true));
    }
}
