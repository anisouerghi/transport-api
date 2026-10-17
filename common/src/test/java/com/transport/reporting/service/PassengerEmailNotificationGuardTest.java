package com.transport.reporting.service;

import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerPreference;
import com.transport.reporting.repository.PassengerPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerEmailNotificationGuardTest {

    @Mock
    private PassengerPreferenceRepository preferenceRepository;

    private PassengerEmailNotificationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PassengerEmailNotificationGuard(preferenceRepository);
    }

    @Test
    void allowsWhenNotifyEmailTrue() {
        when(preferenceRepository.findByPassenger_PassengerId(1L))
                .thenReturn(Optional.of(preference(true)));
        assertEquals(PassengerEmailNotificationGuard.Decision.ALLOW, guard.evaluate(1L));
    }

    @Test
    void deniesWhenNotifyEmailFalse() {
        when(preferenceRepository.findByPassenger_PassengerId(2L))
                .thenReturn(Optional.of(preference(false)));
        assertEquals(PassengerEmailNotificationGuard.Decision.DENY_DISABLED, guard.evaluate(2L));
    }

    @Test
    void allowsWhenPreferenceMissingForCompatibility() {
        when(preferenceRepository.findByPassenger_PassengerId(3L)).thenReturn(Optional.empty());
        assertEquals(PassengerEmailNotificationGuard.Decision.ALLOW, guard.evaluate(3L));
    }

    @Test
    void allowsAnonymousPassengerWithoutId() {
        assertEquals(PassengerEmailNotificationGuard.Decision.ALLOW, guard.evaluate((Passenger) null));
        assertEquals(PassengerEmailNotificationGuard.Decision.ALLOW, guard.evaluate((Long) null));
    }

    @Test
    void deniesSafelyWhenPreferenceReadFails() {
        when(preferenceRepository.findByPassenger_PassengerId(9L))
                .thenThrow(new RuntimeException("db down"));
        assertEquals(PassengerEmailNotificationGuard.Decision.DENY_ERROR, guard.evaluate(9L));
    }

    @Test
    void twoPassengersAreIndependent() {
        when(preferenceRepository.findByPassenger_PassengerId(10L))
                .thenReturn(Optional.of(preference(true)));
        when(preferenceRepository.findByPassenger_PassengerId(11L))
                .thenReturn(Optional.of(preference(false)));
        assertEquals(PassengerEmailNotificationGuard.Decision.ALLOW, guard.evaluate(10L));
        assertEquals(PassengerEmailNotificationGuard.Decision.DENY_DISABLED, guard.evaluate(11L));
    }

    private static PassengerPreference preference(boolean email) {
        return PassengerPreference.builder()
                .preferenceId(1L)
                .language("FR")
                .notifyEmail(email)
                .notifyPlatform(true)
                .build();
    }
}
