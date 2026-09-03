package com.transport.reporting.service;

import com.transport.reporting.config.OtpProperties;
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

/**
 * Authentification publique des voyageurs.
 * {@code password_hash == null} → contact anonyme ; renseigné → compte inscrit.
 */
@Service
@Transactional
public class PassengerAuthService {

    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpProperties otpProperties;
    private final PassengerOtpService passengerOtpService;

    public PassengerAuthService(
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpProperties otpProperties,
            PassengerOtpService passengerOtpService) {
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpProperties = otpProperties;
        this.passengerOtpService = passengerOtpService;
    }

    public PassengerAuthResponse register(PassengerRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        var existing = passengerRepository.findByEmailIgnoreCase(email);

        Passenger passenger;
        if (existing.isPresent()) {
            passenger = existing.get();
            if (StringUtils.hasText(passenger.getGoogleSubject())) {
                throw new BusinessException("Un compte Google existe déjà avec cet e-mail. Connectez-vous avec Google.");
            }
            if (StringUtils.hasText(passenger.getPasswordHash())) {
                throw new BusinessException("Un compte existe déjà avec cet e-mail. Connectez-vous.");
            }
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

        passenger = passengerRepository.save(passenger);
        return toAuthResponse(passenger);
    }

    /**
     * Connexion e-mail/mot de passe. Si OTP activé, ne délivre pas de JWT avant validation OTP.
     */
    public PassengerLoginResult login(PassengerLoginRequest request) {
        Passenger passenger = authenticateLocalCredentials(request);
        if (!otpProperties.isEnabled()) {
            return PassengerLoginResult.jwt(toAuthResponse(passenger));
        }
        return PassengerLoginResult.otpRequired(passengerOtpService.startChallenge(passenger));
    }

    public PassengerAuthResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        Passenger passenger = passengerOtpService.verifyChallenge(request);
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
            String fullName) {
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

        if (!passenger.isActive()) {
            throw new BusinessException("Ce compte voyageur est désactivé.");
        }

        passenger = passengerRepository.save(passenger);
        return toAuthResponse(passenger);
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
        return response;
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
