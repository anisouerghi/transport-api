package com.transport.reporting.service;

import com.transport.reporting.config.OtpProperties;
import com.transport.reporting.dto.PassengerLoginRequest;
import com.transport.reporting.dto.PassengerOtpPendingResponse;
import com.transport.reporting.dto.PassengerProfileUpdateRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.security.JwtService;
import com.transport.reporting.security.PassengerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerAuthServiceOtpTest {

    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PassengerOtpService passengerOtpService;

    private PasswordEncoder passwordEncoder;
    private PassengerAuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new PassengerAuthService(
                passengerRepository,
                passwordEncoder,
                jwtService,
                passengerOtpService);
    }

    @Test
    void login_whenEmailIsVerified_returnsJwtDirectly() {
        Passenger passenger = activePassenger("secret123");
        passenger.setEmailVerified(true);
        when(passengerRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(passenger));
        when(jwtService.generatePassengerToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        PassengerLoginRequest request = new PassengerLoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("secret123");

        var result = authService.login(request);

        assertThat(result.isOtpRequired()).isFalse();
        assertThat(result.getAuthResponse().getToken()).isEqualTo("jwt-token");
        verify(passengerOtpService, never()).startChallenge(any());
    }

    @Test
    void login_whenEmailIsNotVerified_sendsOtpWithoutJwt() {
        Passenger passenger = activePassenger("secret123");
        when(passengerRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(passenger));
        when(passengerOtpService.startChallenge(passenger)).thenReturn(
                PassengerOtpPendingResponse.builder()
                        .otpTransactionId("tx-123")
                        .expiresInSeconds(300)
                        .resendDelaySeconds(60)
                        .build());

        PassengerLoginRequest request = new PassengerLoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("secret123");

        var result = authService.login(request);

        assertThat(result.isOtpRequired()).isTrue();
        assertThat(result.getOtpPending().getOtpTransactionId()).isEqualTo("tx-123");
        verify(passengerOtpService).startChallenge(passenger);
        verify(jwtService, never()).generatePassengerToken(any());
    }

    @Test
    void updateProfile_whenPasswordIsBlank_keepsExistingPasswordHash() {
        Passenger passenger = activePassenger("secret123");
        String existingHash = passenger.getPasswordHash();
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(passenger)).thenReturn(passenger);
        when(jwtService.generatePassengerToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        PassengerProfileUpdateRequest request = new PassengerProfileUpdateRequest();
        request.setName("Updated name");
        request.setPassword("");

        authService.updateProfile(
                new PassengerPrincipal(1L, passenger.getEmail(), passenger.getName(), null, true),
                request);

        assertThat(passenger.getName()).isEqualTo("Updated name");
        assertThat(passenger.getPasswordHash()).isEqualTo(existingHash);
        verify(passengerRepository).save(passenger);
    }

    @Test
    void updateProfile_whenNewPasswordIsProvided_requiresCurrentPassword() {
        Passenger passenger = activePassenger("secret123");
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        PassengerProfileUpdateRequest request = new PassengerProfileUpdateRequest();
        request.setPassword("newsecret123");

        assertThatThrownBy(() -> authService.updateProfile(
                new PassengerPrincipal(1L, passenger.getEmail(), passenger.getName(), null, true),
                request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ancien mot de passe incorrect.");

        verify(passengerRepository, never()).save(any());
    }

    private Passenger activePassenger(String rawPassword) {
        Passenger passenger = new Passenger();
        passenger.setPassengerId(1L);
        passenger.setEmail("user@test.com");
        passenger.setPasswordHash(passwordEncoder.encode(rawPassword));
        passenger.setActive(true);
        return passenger;
    }
}
