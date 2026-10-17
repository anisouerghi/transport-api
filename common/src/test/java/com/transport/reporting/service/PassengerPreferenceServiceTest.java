package com.transport.reporting.service;

import com.transport.reporting.common.enums.PassengerLanguage;
import com.transport.reporting.dto.PassengerPreferenceUpdateRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerPreference;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.PassengerPreferenceRepository;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.security.PassengerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerPreferenceServiceTest {

    @Mock
    private PassengerPreferenceRepository preferenceRepository;
    @Mock
    private PassengerRepository passengerRepository;

    private PassengerPreferenceService service;
    private Passenger passenger;
    private PassengerPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new PassengerPreferenceService(preferenceRepository, passengerRepository);
        passenger = new Passenger();
        passenger.setPassengerId(10L);
        passenger.setEmail("voyageur@example.com");
        passenger.setPasswordHash("hash");
        passenger.setActive(true);
        principal = new PassengerPrincipal(10L, "voyageur@example.com", "Voyageur", null, true);
    }

    @Test
    void getCreatesDefaultsWhenMissing() {
        when(passengerRepository.findById(10L)).thenReturn(Optional.of(passenger));
        when(preferenceRepository.findByPassenger_PassengerId(10L)).thenReturn(Optional.empty());
        when(preferenceRepository.saveAndFlush(any(PassengerPreference.class))).thenAnswer(inv -> {
            PassengerPreference pref = inv.getArgument(0);
            pref.setPreferenceId(1L);
            return pref;
        });
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.getForCurrent(principal);

        assertEquals(PassengerLanguage.FR, response.getLanguage());
        assertEquals(List.of(1, 2), response.getNotifications());
        assertTrue(response.isNotifyPlatform());
        assertTrue(response.isNotifyEmail());
        assertFalse(response.isNotifySms());
    }

    @Test
    void seedAccountDefaultsUsesFrenchAndEmail() {
        when(preferenceRepository.findByPassenger_PassengerId(10L)).thenReturn(Optional.empty());
        when(preferenceRepository.saveAndFlush(any(PassengerPreference.class))).thenAnswer(inv -> {
            PassengerPreference pref = inv.getArgument(0);
            pref.setPreferenceId(1L);
            return pref;
        });
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0));

        PassengerPreference pref = service.seedAccountDefaults(passenger);

        assertEquals(PassengerLanguage.FR, pref.getLanguage());
        assertTrue(pref.isNotifyEmail());
        assertEquals(PassengerLanguage.FR, passenger.getLanguage());
        assertTrue(passenger.getNotifications().contains(2));
    }

    @Test
    void updateLanguageToArabic() {
        PassengerPreference existing = PassengerPreference.builder()
                .preferenceId(1L)
                .passenger(passenger)
                .language(PassengerLanguage.FR)
                .notifyPlatform(true)
                .notifyEmail(true)
                .build();
        when(passengerRepository.findById(10L)).thenReturn(Optional.of(passenger));
        when(preferenceRepository.findByPassenger_PassengerId(10L)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(PassengerPreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0));

        PassengerPreferenceUpdateRequest request = new PassengerPreferenceUpdateRequest();
        request.setLanguage("ar");
        var response = service.updateForCurrent(principal, request);

        assertEquals(PassengerLanguage.AR, response.getLanguage());
        ArgumentCaptor<Passenger> passengerCaptor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(passengerCaptor.capture());
        assertEquals(PassengerLanguage.AR, passengerCaptor.getValue().getLanguage());
    }

    @Test
    void rejectsInvalidLanguage() {
        PassengerPreference existing = PassengerPreference.builder()
                .preferenceId(1L)
                .passenger(passenger)
                .language(PassengerLanguage.FR)
                .build();
        when(passengerRepository.findById(10L)).thenReturn(Optional.of(passenger));
        when(preferenceRepository.findByPassenger_PassengerId(10L)).thenReturn(Optional.of(existing));

        PassengerPreferenceUpdateRequest request = new PassengerPreferenceUpdateRequest();
        request.setLanguage("de");
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateForCurrent(principal, request));
        assertEquals(PassengerLanguage.ERROR_CODE, ex.getErrorCode());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void rejectsUnauthenticated() {
        assertThrows(BusinessException.class, () -> service.getForCurrent(null));
    }
}
