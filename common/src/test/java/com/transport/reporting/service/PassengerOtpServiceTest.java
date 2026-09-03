package com.transport.reporting.service;

import com.transport.reporting.config.OtpProperties;
import com.transport.reporting.dto.EmailSendResult;
import com.transport.reporting.dto.OtpResendRequest;
import com.transport.reporting.dto.OtpVerifyRequest;
import com.transport.reporting.entity.OtpChallengeStatus;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerOtpChallenge;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.PassengerOtpChallengeRepository;
import com.transport.reporting.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerOtpServiceTest {

    @Mock
    private PassengerOtpChallengeRepository challengeRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpEmailComposer otpEmailComposer;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private OtpProperties otpProperties;
    private PassengerOtpService service;

    @BeforeEach
    void setUp() {
        otpProperties = new OtpProperties();
        otpProperties.setLength(6);
        otpProperties.setExpirationMinutes(5);
        otpProperties.setMaxAttempts(5);
        otpProperties.setResendDelaySeconds(60);
        otpProperties.setMaxResendsPerChallenge(5);

        service = new PassengerOtpService(
                otpProperties,
                challengeRepository,
                passengerRepository,
                passwordEncoder,
                emailService,
                otpEmailComposer);
    }

    @Test
    void maskEmail_hidesLocalPart() {
        assertThat(PassengerOtpService.maskEmail("voyageur@example.com")).isEqualTo("v***@example.com");
    }

    @Test
    void startChallenge_sendsEmailAndReturnsTransactionId() {
        Passenger passenger = passenger(1L, "user@test.com");
        when(challengeRepository.save(any())).thenAnswer(inv -> {
            PassengerOtpChallenge c = inv.getArgument(0);
            c.setChallengeId(10L);
            return c;
        });
        when(emailService.sendHtml(anyString(), anyString(), anyString())).thenReturn(EmailSendResult.ok("ok"));
        when(otpEmailComposer.buildHtml(anyString(), anyInt())).thenReturn("<html></html>");

        var pending = service.startChallenge(passenger);

        assertThat(pending.getOtpTransactionId()).isNotBlank();
        assertThat(pending.getMaskedEmail()).contains("@test.com");
        verify(emailService).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void verifyChallenge_rejectsExpiredCode() {
        String txId = UUID.randomUUID().toString();
        PassengerOtpChallenge challenge = challenge(txId, 1L, "123456", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(challengeRepository.findByTransactionId(txId)).thenReturn(Optional.of(challenge));

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setOtpTransactionId(txId);
        request.setOtp("123456");

        assertThatThrownBy(() -> service.verifyChallenge(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("OTP_EXPIRED");
    }

    @Test
    void verifyChallenge_rejectsWrongCode() {
        String txId = UUID.randomUUID().toString();
        PassengerOtpChallenge challenge = challenge(txId, 1L, "123456", Instant.now().plus(5, ChronoUnit.MINUTES));
        when(challengeRepository.findByTransactionId(txId)).thenReturn(Optional.of(challenge));

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setOtpTransactionId(txId);
        request.setOtp("000000");

        assertThatThrownBy(() -> service.verifyChallenge(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("OTP_INVALID");

        assertThat(challenge.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void verifyChallenge_issuesPassengerOnSuccess() {
        String txId = UUID.randomUUID().toString();
        String otp = "654321";
        PassengerOtpChallenge challenge = challenge(txId, 1L, otp, Instant.now().plus(5, ChronoUnit.MINUTES));
        Passenger passenger = passenger(1L, "user@test.com");

        when(challengeRepository.findByTransactionId(txId)).thenReturn(Optional.of(challenge));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setOtpTransactionId(txId);
        request.setOtp(otp);

        Passenger verified = service.verifyChallenge(request);

        assertThat(verified.getEmail()).isEqualTo("user@test.com");
        assertThat(challenge.getStatus()).isEqualTo(OtpChallengeStatus.VERIFIED);
    }

    @Test
    void resendChallenge_blocksTooSoon() {
        String txId = UUID.randomUUID().toString();
        PassengerOtpChallenge challenge = challenge(txId, 1L, "123456", Instant.now().plus(5, ChronoUnit.MINUTES));
        challenge.setLastSentAt(Instant.now());

        when(challengeRepository.findByTransactionId(txId)).thenReturn(Optional.of(challenge));

        OtpResendRequest request = new OtpResendRequest();
        request.setOtpTransactionId(txId);

        assertThatThrownBy(() -> service.resendChallenge(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("OTP_RESEND_TOO_SOON");
    }

    @Test
    void startChallenge_continuesWhenSmtpUnavailable() {
        Passenger passenger = passenger(1L, "user@test.com");
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(otpEmailComposer.buildHtml(anyString(), anyInt())).thenReturn("<html></html>");
        when(emailService.sendHtml(anyString(), anyString(), anyString()))
                .thenReturn(EmailSendResult.fail("EMAIL_SMTP_UNAVAILABLE", "SMTP down"));

        var pending = service.startChallenge(passenger);

        assertThat(pending.getOtpTransactionId()).isNotBlank();
        assertThat(pending.isEmailSent()).isFalse();
    }

    private static Passenger passenger(long id, String email) {
        Passenger p = new Passenger();
        p.setPassengerId(id);
        p.setEmail(email);
        p.setActive(true);
        return p;
    }

    private PassengerOtpChallenge challenge(String txId, long passengerId, String otp, Instant expiresAt) {
        return PassengerOtpChallenge.builder()
                .transactionId(txId)
                .passengerId(passengerId)
                .otpHash(passwordEncoder.encode(otp))
                .attemptCount(0)
                .sendCount(1)
                .expiresAt(expiresAt)
                .lastSentAt(Instant.now().minus(120, ChronoUnit.SECONDS))
                .status(OtpChallengeStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
