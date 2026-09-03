package com.transport.reporting.service;

import com.transport.reporting.config.OtpProperties;
import com.transport.reporting.dto.EmailSendResult;
import com.transport.reporting.dto.OtpResendRequest;
import com.transport.reporting.dto.OtpVerifyRequest;
import com.transport.reporting.dto.PassengerOtpPendingResponse;
import com.transport.reporting.entity.OtpChallengeStatus;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerOtpChallenge;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.PassengerOtpChallengeRepository;
import com.transport.reporting.repository.PassengerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gestion du cycle de vie OTP e-mail pour la connexion voyageur.
 * Le code n'est jamais journalisé ni renvoyé dans les réponses HTTP.
 */
@Service
@Transactional
public class PassengerOtpService {

    private static final Logger log = LoggerFactory.getLogger(PassengerOtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpProperties otpProperties;
    private final PassengerOtpChallengeRepository challengeRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpEmailComposer otpEmailComposer;

    public PassengerOtpService(
            OtpProperties otpProperties,
            PassengerOtpChallengeRepository challengeRepository,
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OtpEmailComposer otpEmailComposer) {
        this.otpProperties = otpProperties;
        this.challengeRepository = challengeRepository;
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpEmailComposer = otpEmailComposer;
    }

    /**
     * Crée une transaction OTP, envoie le code par e-mail et retourne les métadonnées (sans le code).
     */
    public PassengerOtpPendingResponse startChallenge(Passenger passenger) {
        challengeRepository.cancelPendingForPassenger(
                passenger.getPassengerId(), OtpChallengeStatus.PENDING, OtpChallengeStatus.CANCELLED);

        String otpCode = generateOtpCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(otpProperties.getExpirationMinutes()));

        PassengerOtpChallenge challenge = PassengerOtpChallenge.builder()
                .transactionId(UUID.randomUUID().toString())
                .passengerId(passenger.getPassengerId())
                .otpHash(passwordEncoder.encode(otpCode))
                .attemptCount(0)
                .sendCount(1)
                .expiresAt(expiresAt)
                .lastSentAt(now)
                .status(OtpChallengeStatus.PENDING)
                .createdAt(now)
                .build();
        challenge = challengeRepository.save(challenge);

        boolean emailSent = dispatchOtpEmail(passenger, otpCode);

        log.info("OTP challenge créé transactionId={} passengerId={} emailSent={}",
                challenge.getTransactionId(), passenger.getPassengerId(), emailSent);

        return toPendingResponse(challenge, passenger.getEmail(), emailSent);
    }

    /**
     * Vérifie le code OTP et retourne le voyageur associé si valide.
     */
    @Transactional
    public Passenger verifyChallenge(OtpVerifyRequest request) {
        PassengerOtpChallenge challenge = loadPendingChallenge(request.getOtpTransactionId());
        ensureNotExpired(challenge);

        if (challenge.getAttemptCount() >= otpProperties.getMaxAttempts()) {
            challenge.setStatus(OtpChallengeStatus.LOCKED);
            challengeRepository.save(challenge);
            throw new BusinessException(
                    "Nombre maximal de tentatives atteint. Reconnectez-vous pour recevoir un nouveau code.",
                    "OTP_TOO_MANY_ATTEMPTS");
        }

        if (!passwordEncoder.matches(request.getOtp().trim(), challenge.getOtpHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            if (challenge.getAttemptCount() >= otpProperties.getMaxAttempts()) {
                challenge.setStatus(OtpChallengeStatus.LOCKED);
            }
            challengeRepository.save(challenge);
            throw new BusinessException("Code de vérification incorrect.", "OTP_INVALID");
        }

        challenge.setStatus(OtpChallengeStatus.VERIFIED);
        challengeRepository.save(challenge);

        return passengerRepository.findById(challenge.getPassengerId())
                .filter(Passenger::isActive)
                .orElseThrow(() -> new BusinessException("Session OTP invalide.", "OTP_TRANSACTION_INVALID"));
    }

    /**
     * Renvoie un nouveau code OTP pour une transaction existante (délai minimum respecté).
     */
    public PassengerOtpPendingResponse resendChallenge(OtpResendRequest request) {
        PassengerOtpChallenge challenge = challengeRepository.findByTransactionId(request.getOtpTransactionId())
                .orElseThrow(() -> new BusinessException("Transaction OTP invalide.", "OTP_TRANSACTION_INVALID"));

        if (challenge.getStatus() != OtpChallengeStatus.PENDING) {
            throw new BusinessException("Cette transaction OTP n'est plus valide.", "OTP_TRANSACTION_INVALID");
        }

        ensureNotExpired(challenge);
        enforceResendDelay(challenge);
        enforceMaxResends(challenge);

        String otpCode = generateOtpCode();
        Instant now = Instant.now();
        challenge.setOtpHash(passwordEncoder.encode(otpCode));
        challenge.setAttemptCount(0);
        challenge.setSendCount(challenge.getSendCount() + 1);
        challenge.setLastSentAt(now);
        challenge.setExpiresAt(now.plus(Duration.ofMinutes(otpProperties.getExpirationMinutes())));
        challengeRepository.save(challenge);

        Passenger passenger = passengerRepository.findById(challenge.getPassengerId())
                .orElseThrow(() -> new BusinessException("Transaction OTP invalide.", "OTP_TRANSACTION_INVALID"));

        sendOtpEmail(passenger, otpCode);

        log.info("OTP renvoyé transactionId={} passengerId={} sendCount={}",
                challenge.getTransactionId(), passenger.getPassengerId(), challenge.getSendCount());

        return toPendingResponse(challenge, passenger.getEmail(), true);
    }

    private PassengerOtpChallenge loadPendingChallenge(String transactionId) {
        PassengerOtpChallenge challenge = challengeRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction OTP invalide.", "OTP_TRANSACTION_INVALID"));

        if (challenge.getStatus() == OtpChallengeStatus.VERIFIED) {
            throw new BusinessException("Ce code a déjà été utilisé.", "OTP_ALREADY_USED");
        }
        if (challenge.getStatus() == OtpChallengeStatus.LOCKED) {
            throw new BusinessException(
                    "Nombre maximal de tentatives atteint. Reconnectez-vous pour recevoir un nouveau code.",
                    "OTP_TOO_MANY_ATTEMPTS");
        }
        if (challenge.getStatus() != OtpChallengeStatus.PENDING) {
            throw new BusinessException("Transaction OTP invalide.", "OTP_TRANSACTION_INVALID");
        }
        return challenge;
    }

    private void ensureNotExpired(PassengerOtpChallenge challenge) {
        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            challenge.setStatus(OtpChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new BusinessException("Le code de vérification a expiré.", "OTP_EXPIRED");
        }
    }

    private void enforceResendDelay(PassengerOtpChallenge challenge) {
        long elapsedSeconds = Duration.between(challenge.getLastSentAt(), Instant.now()).getSeconds();
        if (elapsedSeconds < otpProperties.getResendDelaySeconds()) {
            long remaining = otpProperties.getResendDelaySeconds() - elapsedSeconds;
            throw new BusinessException(
                    "Veuillez patienter " + remaining + " seconde(s) avant de demander un nouveau code.",
                    "OTP_RESEND_TOO_SOON");
        }
    }

    private void enforceMaxResends(PassengerOtpChallenge challenge) {
        if (challenge.getSendCount() >= otpProperties.getMaxResendsPerChallenge()) {
            challenge.setStatus(OtpChallengeStatus.LOCKED);
            challengeRepository.save(challenge);
            throw new BusinessException(
                    "Nombre maximal de renvois atteint. Reconnectez-vous pour recommencer.",
                    "OTP_RESEND_LIMIT");
        }
    }

    /**
     * Tente l'envoi SMTP sans bloquer la création du challenge (écran OTP toujours proposé).
     */
    private boolean dispatchOtpEmail(Passenger passenger, String otpCode) {
        if (!StringUtils.hasText(passenger.getEmail())) {
            log.warn("OTP sans destinataire e-mail passengerId={}", passenger.getPassengerId());
            return false;
        }

        String html = otpEmailComposer.buildHtml(otpCode, otpProperties.getExpirationMinutes());
        EmailSendResult result = emailService.sendHtml(
                passenger.getEmail(), OtpEmailComposer.SUBJECT, html);

        if (!result.isSuccess()) {
            log.error("Échec envoi OTP e-mail passengerId={} smtpError={} to={}",
                    passenger.getPassengerId(), result.getErrorCode(), maskEmail(passenger.getEmail()));
            return false;
        }
        return true;
    }

    private void sendOtpEmail(Passenger passenger, String otpCode) {
        if (!StringUtils.hasText(passenger.getEmail())) {
            throw new BusinessException("Aucune adresse e-mail associée à ce compte.", "OTP_EMAIL_MISSING");
        }

        if (!dispatchOtpEmail(passenger, otpCode)) {
            throw new BusinessException(
                    "Impossible d'envoyer le code de vérification. Réessayez dans quelques instants.",
                    "OTP_EMAIL_SEND_FAILED");
        }
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpProperties.getLength());
        int floor = bound / 10;
        int value = SECURE_RANDOM.nextInt(bound - floor) + floor;
        return String.format("%0" + otpProperties.getLength() + "d", value);
    }

    private PassengerOtpPendingResponse toPendingResponse(
            PassengerOtpChallenge challenge, String email, boolean emailSent) {
        long expiresInSeconds = Math.max(0, Duration.between(Instant.now(), challenge.getExpiresAt()).getSeconds());
        return PassengerOtpPendingResponse.builder()
                .otpTransactionId(challenge.getTransactionId())
                .expiresInSeconds((int) expiresInSeconds)
                .resendDelaySeconds(otpProperties.getResendDelaySeconds())
                .maskedEmail(maskEmail(email))
                .emailSent(emailSent)
                .build();
    }

    static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String normalized = email.trim();
        int at = normalized.indexOf('@');
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at);
        if (local.length() <= 1) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
