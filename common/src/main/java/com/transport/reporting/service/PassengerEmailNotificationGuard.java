package com.transport.reporting.service;

import com.transport.reporting.entity.Passenger;
import com.transport.reporting.repository.PassengerPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrôle centralisé avant envoi d'une notification e-mail <strong>facultative</strong>
 * au voyageur ({@code notify_email} dans {@code passenger_preference}).
 * <p>
 * Ne s'applique pas aux e-mails d'authentification (OTP, etc.).
 * </p>
 * <ul>
 *   <li>{@code notify_email = true} → autoriser ;</li>
 *   <li>{@code notify_email = false} → ignorer l'envoi ;</li>
 *   <li>aucune ligne de préférences → autoriser (compatibilité : historique = envoi systématique) ;</li>
 *   <li>erreur de lecture → refuser l'envoi (comportement sûr) et journaliser.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PassengerEmailNotificationGuard {

    private static final Logger log = LoggerFactory.getLogger(PassengerEmailNotificationGuard.class);

    public static final String SKIP_CODE = "EMAIL_PREFERENCE_DISABLED";
    public static final String SKIP_MESSAGE =
            "Notification e-mail non envoyée : le voyageur a désactivé les notifications par e-mail.";
    public static final String ERROR_SKIP_CODE = "EMAIL_PREFERENCE_UNAVAILABLE";
    public static final String ERROR_SKIP_MESSAGE =
            "Notification e-mail non envoyée : impossible de vérifier la préférence voyageur.";

    private final PassengerPreferenceRepository preferenceRepository;

    public PassengerEmailNotificationGuard(PassengerPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public Decision evaluate(Passenger passenger) {
        if (passenger == null || passenger.getPassengerId() == null) {
            return Decision.ALLOW;
        }
        return evaluate(passenger.getPassengerId());
    }

    public Decision evaluate(Long passengerId) {
        if (passengerId == null) {
            return Decision.ALLOW;
        }
        try {
            return preferenceRepository.findByPassenger_PassengerId(passengerId)
                    .map(pref -> pref.isNotifyEmail() ? Decision.ALLOW : Decision.DENY_DISABLED)
                    .orElseGet(() -> {
                        log.debug(
                                "Pas de passenger_preference pour passengerId={} — notification e-mail autorisée (compat).",
                                passengerId);
                        return Decision.ALLOW;
                    });
        } catch (Exception ex) {
            log.error(
                    "Lecture préférences e-mail impossible pour passengerId={} — envoi notification bloqué (safe).",
                    passengerId,
                    ex);
            return Decision.DENY_ERROR;
        }
    }

    /** Raccourci : {@code true} uniquement si l'envoi est autorisé. */
    public boolean allowsOptionalEmail(Passenger passenger) {
        return evaluate(passenger) == Decision.ALLOW;
    }

    public boolean allowsOptionalEmail(Long passengerId) {
        return evaluate(passengerId) == Decision.ALLOW;
    }

    public enum Decision {
        ALLOW,
        DENY_DISABLED,
        DENY_ERROR;

        public String errorCode() {
            return switch (this) {
                case DENY_DISABLED -> SKIP_CODE;
                case DENY_ERROR -> ERROR_SKIP_CODE;
                case ALLOW -> null;
            };
        }

        public String message() {
            return switch (this) {
                case DENY_DISABLED -> SKIP_MESSAGE;
                case DENY_ERROR -> ERROR_SKIP_MESSAGE;
                case ALLOW -> null;
            };
        }
    }
}
