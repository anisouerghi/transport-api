package com.transport.reporting.service;

import com.transport.reporting.entity.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Correspondance statut interne → statut public voyageur.
 * Les codes métier restants sont ceux de la table {@code report_status}
 * ({@code NEW}, {@code AFFECTE}, {@code IN_PROGRESS}, …).
 */
@Component
public class PublicStatusMapper {

    /** Libellés publics de référence (FR). */
    public static final String RECEIVED = "Reçu";
    public static final String IN_TREATMENT = "En cours de traitement";
    public static final String COMPLEMENT_ASKED = "Informations complémentaires demandées";
    public static final String TREATED = "Traité";
    public static final String CLOSED = "Clôturé";
    public static final String REJECTED = "Non retenu";

    private static final Map<String, String> DEFAULTS = Map.of(
            "NEW", RECEIVED,
            "AFFECTE", IN_TREATMENT,
            "IN_PROGRESS", IN_TREATMENT,
            "DEMANDE_COMPLEMENT", COMPLEMENT_ASKED,
            "DEMANDE_CLARIFICATION", COMPLEMENT_ASKED,
            "RESOLVED", TREATED,
            "CLOSED", CLOSED,
            "REJETE", REJECTED
    );

    private static final Map<String, String> DEFAULT_MESSAGES = Map.of(
            RECEIVED, "Votre signalement a bien été reçu par TRANSTU.",
            IN_TREATMENT, "Votre signalement est en cours de traitement par les services concernés.",
            COMPLEMENT_ASKED,
            "Des informations complémentaires sont nécessaires pour poursuivre le traitement de votre signalement. "
                    + "Consultez votre dossier pour connaître les informations demandées.",
            TREATED, "Le traitement de votre signalement est terminé. Consultez votre dossier pour connaître la réponse.",
            CLOSED, "Votre signalement a été clôturé. Consultez votre dossier pour plus de détails.",
            REJECTED, "Votre signalement n'a pas été retenu. Consultez votre dossier pour connaître le motif communiqué."
    );

    /**
     * Utilise {@code statut_public} en base s'il est renseigné, sinon la table de correspondance.
     */
    public String publicLabel(Status status) {
        if (status == null) {
            return null;
        }
        if (StringUtils.hasText(status.getStatutPublic())) {
            return status.getStatutPublic().trim();
        }
        return publicLabel(status.getCode());
    }

    public String publicLabel(String internalCode) {
        if (!StringUtils.hasText(internalCode)) {
            return null;
        }
        return DEFAULTS.getOrDefault(internalCode.trim().toUpperCase(), internalCode.trim());
    }

    /** Message d'information lié au statut public, avant personnalisation par l'agent. */
    public String defaultNotificationMessage(String publicLabel) {
        if (!StringUtils.hasText(publicLabel)) {
            return "Le statut de votre signalement a été mis à jour. Consultez votre dossier pour plus de détails.";
        }
        return DEFAULT_MESSAGES.getOrDefault(publicLabel.trim(),
                "Le statut de votre signalement est passé à « " + publicLabel.trim()
                        + " ». Consultez votre dossier pour plus de détails.");
    }

    /**
     * Une transition ne notifie que si le statut public change
     * (ex. {@code AFFECTE} → {@code IN_PROGRESS} : même libellé public, pas de second envoi).
     */
    public boolean publicStatusChanged(String previousPublic, String nextPublic) {
        if (!StringUtils.hasText(nextPublic)) {
            return false;
        }
        if (!StringUtils.hasText(previousPublic)) {
            return true;
        }
        return !previousPublic.trim().equalsIgnoreCase(nextPublic.trim());
    }
}
