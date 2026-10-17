package com.transport.reporting.service;

import com.transport.reporting.common.enums.PassengerLanguage;
import com.transport.reporting.common.enums.PassengerNotificationChannel;
import com.transport.reporting.dto.PassengerPreferenceResponse;
import com.transport.reporting.dto.PassengerPreferenceUpdateRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerPreference;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.repository.PassengerPreferenceRepository;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.security.PassengerPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Préférences voyageur : table {@code passenger_preference} = source de vérité.
 * Initialise avec les défauts (ou les anciennes colonnes {@code passenger}) à la première lecture.
 */
@Service
@Transactional
public class PassengerPreferenceService {

    private final PassengerPreferenceRepository preferenceRepository;
    private final PassengerRepository passengerRepository;

    public PassengerPreferenceService(
            PassengerPreferenceRepository preferenceRepository,
            PassengerRepository passengerRepository) {
        this.preferenceRepository = preferenceRepository;
        this.passengerRepository = passengerRepository;
    }

    @Transactional(readOnly = true)
    public PassengerPreferenceResponse getForCurrent(PassengerPrincipal principal) {
        Passenger passenger = requireRegisteredPassenger(principal);
        return toResponse(getOrCreate(passenger));
    }

    public PassengerPreferenceResponse updateForCurrent(
            PassengerPrincipal principal,
            PassengerPreferenceUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("Requête de préférences invalide.");
        }
        Passenger passenger = requireRegisteredPassenger(principal);
        PassengerPreference preference = getOrCreate(passenger);

        if (request.getLanguage() != null) {
            String language = PassengerLanguage.normalizeOrThrow(request.getLanguage());
            if (language == null) {
                throw new BusinessException(PassengerLanguage.INVALID_MESSAGE, PassengerLanguage.ERROR_CODE);
            }
            preference.setLanguage(language);
        }
        if (request.getNotifications() != null) {
            applyNotificationIds(preference, PassengerNotificationChannel.normalizeIds(request.getNotifications()));
        }

        PassengerPreference saved = preferenceRepository.save(preference);
        mirrorOntoPassenger(passenger, saved);
        return toResponse(saved);
    }

    /**
     * Garantit une ligne de préférences (création + défauts si besoin).
     */
    public PassengerPreference ensureForPassenger(Passenger passenger) {
        return getOrCreate(passenger);
    }

    /**
     * À appeler à la création d'un compte voyageur : langue {@code FR} + notifications
     * e-mail (et plateforme) si aucune préférence n'existe encore.
     */
    public PassengerPreference seedAccountDefaults(Passenger passenger) {
        if (passenger == null || passenger.getPassengerId() == null) {
            throw new BusinessException("Passager invalide pour initialiser les préférences.");
        }
        if (passenger.getLanguage() == null || passenger.getLanguage().isBlank()) {
            passenger.setLanguage(PassengerLanguage.FR);
        }
        if (passenger.getNotifications() == null) {
            passenger.setNotifications(PassengerNotificationChannel.defaultIds());
        }
        return getOrCreate(passenger);
    }

    /** Snapshot pour {@code /me} et autres réponses auth. */
    public PassengerPreferenceResponse snapshotForPassenger(Passenger passenger) {
        return toResponse(getOrCreate(passenger));
    }

    private PassengerPreference getOrCreate(Passenger passenger) {
        Long passengerId = passenger.getPassengerId();
        return preferenceRepository.findByPassenger_PassengerId(passengerId)
                .orElseGet(() -> createInitial(passenger));
    }

    private PassengerPreference createInitial(Passenger passenger) {
        String language = resolveInitialLanguage(passenger);
        List<Integer> channelIds = resolveInitialNotificationIds(passenger);

        PassengerPreference preference = PassengerPreference.builder()
                .passenger(passenger)
                .language(language)
                .build();
        applyNotificationIds(preference, channelIds);

        try {
            PassengerPreference saved = preferenceRepository.saveAndFlush(preference);
            mirrorOntoPassenger(passenger, saved);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Course entre deux requêtes simultanées : relire la ligne créée.
            return preferenceRepository.findByPassenger_PassengerId(passenger.getPassengerId())
                    .orElseThrow(() -> ex);
        }
    }

    private static String resolveInitialLanguage(Passenger passenger) {
        String legacy = passenger.getLanguage();
        if (legacy != null && !legacy.isBlank()) {
            try {
                String normalized = PassengerLanguage.normalize(legacy);
                if (normalized != null) {
                    return normalized;
                }
            } catch (IllegalArgumentException ignored) {
                // fallback défaut
            }
        }
        return PassengerLanguage.FR;
    }

    private static List<Integer> resolveInitialNotificationIds(Passenger passenger) {
        List<Integer> legacy = passenger.getNotifications();
        if (legacy != null) {
            try {
                return PassengerNotificationChannel.normalizeIds(legacy);
            } catch (BusinessException ignored) {
                // fallback défaut
            }
        }
        return PassengerNotificationChannel.defaultIds();
    }

    private static void applyNotificationIds(PassengerPreference preference, List<Integer> ids) {
        preference.setNotifyPlatform(ids.contains(PassengerNotificationChannel.PLATFORM.id()));
        preference.setNotifyEmail(ids.contains(PassengerNotificationChannel.EMAIL.id()));
        preference.setNotifySms(ids.contains(PassengerNotificationChannel.SMS.id()));
        preference.setNotifyWhatsapp(ids.contains(PassengerNotificationChannel.WHATSAPP.id()));
    }

    /**
     * Miroir sur {@code passenger.language/notifications} pour l'admin et l'ancien client.
     */
    private void mirrorOntoPassenger(Passenger passenger, PassengerPreference preference) {
        passenger.setLanguage(preference.getLanguage());
        passenger.setNotifications(PassengerNotificationChannel.toIds(
                preference.isNotifyPlatform(),
                preference.isNotifyEmail(),
                preference.isNotifySms(),
                preference.isNotifyWhatsapp()));
        passengerRepository.save(passenger);
    }

    private Passenger requireRegisteredPassenger(PassengerPrincipal principal) {
        if (principal == null || principal.getPassengerId() == null) {
            throw new BusinessException("Authentification requise.");
        }
        Passenger passenger = passengerRepository.findById(principal.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", principal.getPassengerId()));
        if (!passenger.isActive()) {
            throw new BusinessException("Session invalide. Veuillez vous reconnecter.");
        }
        boolean registered = (passenger.getPasswordHash() != null && !passenger.getPasswordHash().isBlank())
                || (passenger.getGoogleSubject() != null && !passenger.getGoogleSubject().isBlank());
        if (!registered) {
            throw new BusinessException("Session invalide. Veuillez vous reconnecter.");
        }
        return passenger;
    }

    private static PassengerPreferenceResponse toResponse(PassengerPreference preference) {
        List<Integer> notifications = PassengerNotificationChannel.toIds(
                preference.isNotifyPlatform(),
                preference.isNotifyEmail(),
                preference.isNotifySms(),
                preference.isNotifyWhatsapp());
        return PassengerPreferenceResponse.builder()
                .preferenceId(preference.getPreferenceId())
                .passengerId(preference.getPassenger().getPassengerId())
                .language(preference.getLanguage())
                .notifications(notifications)
                .notifyPlatform(preference.isNotifyPlatform())
                .notifyEmail(preference.isNotifyEmail())
                .notifySms(preference.isNotifySms())
                .notifyWhatsapp(preference.isNotifyWhatsapp())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
