package com.transport.reporting.service;

import com.transport.reporting.common.util.RequestMetadata;
import com.transport.reporting.common.util.UserAgentParser;
import com.transport.reporting.dto.*;
import com.transport.reporting.entity.AuthProvider;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.security.JwtService;
import com.transport.reporting.security.PassengerPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Authentification publique des voyageurs.
 * {@code password_hash == null} → contact anonyme ; renseigné → compte inscrit.
 */
@Service
@Transactional
public class PassengerAuthService {

    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_PICTURE_URL_LENGTH = 512;

    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PassengerOtpService passengerOtpService;

    public PassengerAuthService(
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PassengerOtpService passengerOtpService) {
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passengerOtpService = passengerOtpService;
    }

    public PassengerOtpPendingResponse register(PassengerRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        var existing = passengerRepository.findByEmailIgnoreCase(email);

        Passenger passenger;
        if (existing.isPresent()) {
            passenger = existing.get();
            if (StringUtils.hasText(passenger.getGoogleSubject())
                    && !StringUtils.hasText(passenger.getPasswordHash())) {
                throw new BusinessException(
                        "Un compte Google existe déjà avec cet e-mail. Connectez-vous avec Google.",
                        "EMAIL_GOOGLE_EXISTS");
            }
            if (passenger.isEmailVerified()) {
                throw new BusinessException(
                        "Un compte existe déjà avec cet e-mail. Connectez-vous.",
                        "EMAIL_ALREADY_EXISTS");
            }
            // Compte local non vérifié : reprendre l'inscription (nouveau mot de passe + OTP)
            if (StringUtils.hasText(request.getName())) {
                passenger.setName(request.getName().trim());
            }
            if (StringUtils.hasText(request.getPhoneNumber())) {
                passenger.setPhoneNumber(request.getPhoneNumber().trim());
            }
            passenger.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            passenger.setAuthProvider(AuthProvider.LOCAL);
            passenger.setActive(true);
        } else {
            passenger = new Passenger();
            passenger.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
            passenger.setEmail(email);
            passenger.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
            passenger.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            passenger.setAuthProvider(AuthProvider.LOCAL);
            passenger.setEmailVerified(false);
            passenger.setActive(true);
        }

        passenger.setEmailVerified(false);
        applyRequestMetadata(passenger);
        applyOptionalGps(passenger, request.getLatitude(), request.getLongitude(), request.getGpsAccuracy());
        passenger = passengerRepository.save(passenger);
        return passengerOtpService.startChallenge(passenger);
    }

    /** Connexion directe si l'e-mail est vérifié, sinon nouveau code OTP. */
    public PassengerLoginResult login(PassengerLoginRequest request) {
        Passenger passenger = authenticateLocalCredentials(request);
        if (!passenger.isEmailVerified()) {
            return PassengerLoginResult.otpRequired(passengerOtpService.startChallenge(passenger));
        }
        applyRequestMetadata(passenger);
        passenger.setLastAuthAt(Instant.now());
        passengerRepository.save(passenger);
        return PassengerLoginResult.jwt(toAuthResponse(passenger));
    }

    public PassengerAuthResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        Passenger passenger = passengerOtpService.verifyChallenge(request);
        passenger.setEmailVerified(true);
        applyRequestMetadata(passenger);
        passenger.setLastAuthAt(Instant.now());
        passengerRepository.save(passenger);
        return toAuthResponse(passenger);
    }

    public PassengerOtpPendingResponse resendOtp(OtpResendRequest request) {
        return passengerOtpService.resendChallenge(request);
    }

    /**
     * Authentifie ou crée un voyageur après validation Google côté serveur (OIDC).
     * Les claims proviennent du {@code OidcUser} Spring Security, jamais du navigateur seul.
     * Google OAuth ne déclenche pas d'OTP (fournisseur d'identité externe déjà vérifié).
     */
    public PassengerAuthResponse authenticateGoogleUser(
            String googleSubject,
            String email,
            boolean emailVerified,
            String fullName,
            String profilePictureUrl) {
        if (!StringUtils.hasText(googleSubject)) {
            throw new BusinessException("Identité Google invalide.");
        }
        if (!emailVerified) {
            throw new BusinessException("Votre adresse e-mail Google n'est pas vérifiée.");
        }
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Aucune adresse e-mail fournie par Google.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Passenger passenger = passengerRepository.findByGoogleSubject(googleSubject.trim())
                .orElseGet(() -> resolvePassengerForGoogle(normalizedEmail, googleSubject.trim()));

        if (StringUtils.hasText(fullName)) {
            passenger.setName(fullName.trim());
        }
        passenger.setEmail(normalizedEmail);
        passenger.setGoogleSubject(googleSubject.trim());
        passenger.setAuthProvider(AuthProvider.GOOGLE);
        passenger.setEmailVerified(true);
        if (StringUtils.hasText(profilePictureUrl)) {
            String picture = profilePictureUrl.trim();
            if (picture.length() > MAX_PICTURE_URL_LENGTH) {
                picture = picture.substring(0, MAX_PICTURE_URL_LENGTH);
            }
            passenger.setProfilePictureUrl(picture);
        }

        if (!passenger.isActive()) {
            throw new BusinessException("Ce compte voyageur est désactivé.");
        }

        applyRequestMetadata(passenger);
        passenger.setLastAuthAt(Instant.now());
        passenger = passengerRepository.save(passenger);
        return toAuthResponse(passenger);
    }

    /**
     * Enrichit un voyageur avec une position GPS optionnelle (callback Google, etc.).
     * Sans effet si latitude/longitude absents ou invalides.
     */
    public void enrichOptionalGps(Long passengerId, Double latitude, Double longitude, Double gpsAccuracy) {
        if (passengerId == null || latitude == null || longitude == null) {
            return;
        }
        passengerRepository.findById(passengerId).ifPresent(passenger -> {
            applyOptionalGps(passenger, latitude, longitude, gpsAccuracy);
            passengerRepository.save(passenger);
        });
    }

    private Passenger authenticateLocalCredentials(PassengerLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Passenger passenger = passengerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("E-mail ou mot de passe incorrect."));

        if (!StringUtils.hasText(passenger.getPasswordHash())) {
            throw new BusinessException("E-mail ou mot de passe incorrect.");
        }
        if (!passenger.isActive()) {
            throw new BusinessException("Ce compte voyageur est désactivé.");
        }
        if (!passwordEncoder.matches(request.getPassword(), passenger.getPasswordHash())) {
            throw new BusinessException("E-mail ou mot de passe incorrect.");
        }
        return passenger;
    }

    private Passenger resolvePassengerForGoogle(String normalizedEmail, String googleSubject) {
        return passengerRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> {
                    if (StringUtils.hasText(existing.getGoogleSubject())
                            && !existing.getGoogleSubject().equals(googleSubject)) {
                        throw new BusinessException("Cet e-mail est associé à un autre compte Google.");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Passenger created = new Passenger();
                    created.setEmail(normalizedEmail);
                    created.setActive(true);
                    created.setPasswordHash(null);
                    return created;
                });
    }

    @Transactional(readOnly = true)
    public PassengerAuthResponse current(PassengerPrincipal principal) {
        if (principal == null) {
            throw new BusinessException("Authentification requise.");
        }
        Passenger passenger = passengerRepository.findById(principal.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", principal.getPassengerId()));
        if (!passenger.isActive() || !isRegisteredAccount(passenger)) {
            throw new BusinessException("Session invalide. Veuillez vous reconnecter.");
        }
        return toAuthResponse(passenger);
    }

    public PassengerAuthResponse updateProfile(
            PassengerPrincipal principal,
            PassengerProfileUpdateRequest request) {
        if (principal == null) {
            throw new BusinessException("Authentification requise.");
        }

        Passenger passenger = passengerRepository.findById(principal.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", principal.getPassengerId()));
        if (!passenger.isActive() || !isRegisteredAccount(passenger)) {
            throw new BusinessException("Session invalide. Veuillez vous reconnecter.");
        }

        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim().toLowerCase();
            passengerRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getPassengerId().equals(passenger.getPassengerId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Un compte existe déjà avec cet e-mail.");
                    });
            passenger.setEmail(email);
        }
        if (request.getName() != null) {
            passenger.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        }
        if (request.getPhoneNumber() != null) {
            passenger.setPhoneNumber(
                    StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
        }

        if (StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().length() < 8 || request.getPassword().length() > 100) {
                throw new BusinessException("Le nouveau mot de passe doit contenir entre 8 et 100 caractères.");
            }
            if (!StringUtils.hasText(passenger.getPasswordHash())
                    || !StringUtils.hasText(request.getCurrentPassword())
                    || !passwordEncoder.matches(request.getCurrentPassword(), passenger.getPasswordHash())) {
                throw new BusinessException("Ancien mot de passe incorrect.");
            }
            passenger.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            passenger.setAuthProvider(AuthProvider.LOCAL);
        }

        return toAuthResponse(passengerRepository.save(passenger));
    }

    PassengerAuthResponse toAuthResponse(Passenger passenger) {
        PassengerPrincipal principal = toPrincipal(passenger);
        String token = jwtService.generatePassengerToken(principal);
        PassengerAuthResponse response = new PassengerAuthResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInMs(jwtService.getExpirationMs());
        response.setPassengerId(passenger.getPassengerId());
        response.setName(passenger.getName());
        response.setEmail(passenger.getEmail());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setProfilePictureUrl(passenger.getProfilePictureUrl());
        response.setAuthProvider(
                passenger.getAuthProvider() != null ? passenger.getAuthProvider().name() : null);
        return response;
    }

    private static void applyRequestMetadata(Passenger passenger) {
        String ip = RequestMetadata.currentIpAddress();
        if (StringUtils.hasText(ip) && !"0.0.0.0".equals(ip)) {
            passenger.setLastIp(ip.length() > 64 ? ip.substring(0, 64) : ip);
        }
        String userAgent = RequestMetadata.currentUserAgent();
        if (StringUtils.hasText(userAgent) && !"unknown".equalsIgnoreCase(userAgent)) {
            passenger.setLastUserAgent(
                    userAgent.length() > MAX_USER_AGENT_LENGTH
                            ? userAgent.substring(0, MAX_USER_AGENT_LENGTH)
                            : userAgent);
            passenger.setLastBrowser(UserAgentParser.detectBrowser(userAgent));
        }
    }

    private static void applyOptionalGps(
            Passenger passenger, Double latitude, Double longitude, Double gpsAccuracy) {
        if (latitude == null || longitude == null) {
            return;
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return;
        }
        passenger.setLatitude(latitude);
        passenger.setLongitude(longitude);
        if (gpsAccuracy != null && gpsAccuracy >= 0) {
            passenger.setGpsAccuracy(gpsAccuracy);
        }
        passenger.setGpsCapturedAt(Instant.now());
    }

    private static boolean isRegisteredAccount(Passenger passenger) {
        return StringUtils.hasText(passenger.getPasswordHash())
                || StringUtils.hasText(passenger.getGoogleSubject());
    }

    private static PassengerPrincipal toPrincipal(Passenger passenger) {
        return new PassengerPrincipal(
                passenger.getPassengerId(),
                passenger.getEmail(),
                passenger.getName(),
                passenger.getPhoneNumber(),
                passenger.isActive());
    }
}
