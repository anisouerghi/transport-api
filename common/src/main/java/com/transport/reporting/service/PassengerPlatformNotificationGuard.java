package com.transport.reporting.service;

import com.transport.reporting.entity.Passenger;
import com.transport.reporting.repository.PassengerPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrôle avant création d'une notification plateforme ({@code notify_platform}).
 * Indépendant de {@link PassengerEmailNotificationGuard} (e-mail inchangé).
 * <ul>
 *   <li>{@code notify_platform = true} → autoriser ;</li>
 *   <li>{@code notify_platform = false} → ignorer ;</li>
 *   <li>aucune ligne de préférences → autoriser (défaut métier) ;</li>
 *   <li>erreur de lecture → refuser (comportement sûr).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PassengerPlatformNotificationGuard {

    private static final Logger log = LoggerFactory.getLogger(PassengerPlatformNotificationGuard.class);

    private final PassengerPreferenceRepository preferenceRepository;

    public PassengerPlatformNotificationGuard(PassengerPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public Decision evaluate(Passenger passenger) {
        if (passenger == null || passenger.getPassengerId() == null) {
            return Decision.DENY_NO_PASSENGER;
        }
        return evaluate(passenger.getPassengerId());
    }

    public Decision evaluate(Long passengerId) {
        if (passengerId == null) {
            return Decision.DENY_NO_PASSENGER;
        }
        try {
            return preferenceRepository.findByPassenger_PassengerId(passengerId)
                    .map(pref -> pref.isNotifyPlatform() ? Decision.ALLOW : Decision.DENY_DISABLED)
                    .orElseGet(() -> {
                        log.debug(
                                "Pas de passenger_preference pour passengerId={} — notif plateforme autorisée (défaut).",
                                passengerId);
                        return Decision.ALLOW;
                    });
        } catch (Exception ex) {
            log.error(
                    "Lecture préférences plateforme impossible pour passengerId={} — création notif bloquée (safe).",
                    passengerId,
                    ex);
            return Decision.DENY_ERROR;
        }
    }

    public enum Decision {
        ALLOW,
        DENY_DISABLED,
        DENY_ERROR,
        DENY_NO_PASSENGER
    }
}
