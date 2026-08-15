package com.transport.reporting.service;

import com.transport.reporting.dto.PassengerAuthResponse;
import com.transport.reporting.dto.PassengerLoginRequest;
import com.transport.reporting.dto.PassengerRegisterRequest;
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
    public PassengerAuthService(PassengerRepository passengerRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    public PassengerAuthResponse register(PassengerRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        var existing = passengerRepository.findByEmailIgnoreCase(email);

        Passenger passenger;
        if (existing.isPresent()) {
            passenger = existing.get();
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
            passenger.setActive(true);
        } else {
            passenger = new Passenger();
            passenger.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
            passenger.setEmail(email);
            passenger.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
            passenger.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            passenger.setEmailVerified(false);
            passenger.setActive(true);
        }

        passenger = passengerRepository.save(passenger);
        return toAuthResponse(passenger);
    }

    public PassengerAuthResponse login(PassengerLoginRequest request) {
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
        return toAuthResponse(passenger);
    }

    @Transactional(readOnly = true)
    public PassengerAuthResponse current(PassengerPrincipal principal) {
        if (principal == null) {
            throw new BusinessException("Authentification requise.");
        }
        Passenger passenger = passengerRepository.findById(principal.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", principal.getPassengerId()));
        if (!passenger.isActive() || !StringUtils.hasText(passenger.getPasswordHash())) {
            throw new BusinessException("Session invalide. Veuillez vous reconnecter.");
        }
        return toAuthResponse(passenger);
    }

    private PassengerAuthResponse toAuthResponse(Passenger passenger) {
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

    private static PassengerPrincipal toPrincipal(Passenger passenger) {
        return new PassengerPrincipal(
                passenger.getPassengerId(),
                passenger.getEmail(),
                passenger.getName(),
                passenger.getPhoneNumber(),
                passenger.isActive());
    }
}
