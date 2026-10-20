package com.transport.reporting.service;

import com.transport.reporting.entity.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Lit le statut public voyageur uniquement depuis {@code report_status.statut_public}.
 * Aucune correspondance code → libellé n'est calculée en Java.
 */
@Component
public class PublicStatusMapper {

    /**
     * @return la valeur de colonne {@code statut_public}, ou {@code null} si absente
     */
    public String publicLabel(Status status) {
        if (status == null || !StringUtils.hasText(status.getStatutPublic())) {
            return null;
        }
        return status.getStatutPublic().trim();
    }

    /** Texte d'information générique lorsque l'agent n'a pas fourni de message public. */
    public String defaultNotificationMessage(String publicLabel) {
        if (!StringUtils.hasText(publicLabel)) {
            return "Le statut de votre signalement a été mis à jour. Consultez votre dossier pour plus de détails.";
        }
        return "Le statut de votre signalement est passé à « " + publicLabel.trim()
                + " ». Consultez votre dossier pour plus de détails.";
    }

    /**
     * Une transition ne notifie que si le statut public (colonne) change
     * (ex. deux codes internes avec le même {@code statut_public} : pas de second envoi).
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
