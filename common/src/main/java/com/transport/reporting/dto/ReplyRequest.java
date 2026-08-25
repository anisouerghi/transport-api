package com.transport.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requête pour créer une réponse à un signalement.
 */
@Data
@Schema(description = "Création d'une réponse agent sur un signalement")
public class ReplyRequest {

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Message de la réponse", example = "Votre signalement est en cours de traitement.")
    private String message;

    /** Optionnel : si absent, utilise l'utilisateur authentifié (JWT). */
    @Schema(description = "Identifiant agent (optionnel, déduit du JWT)")
    private Long userId;

    /** Nouveau statut optionnel du signalement. */
    @Schema(description = "Nouveau statut du signalement (optionnel)")
    private Long statusId;

    /**
     * Si true, tente l'envoi d'un e-mail au voyageur (si adresse disponible)
     * et marque {@code emailSent} sur la réponse.
     */
    @Schema(description = "Envoyer également la réponse par e-mail au voyageur")
    private Boolean sendEmail;

    /** Visible à l'accueil / publication (flag report). */
    @Schema(description = "Publier sur l'accueil (flag signalement)")
    private Boolean publish;

    /**
     * Visibilité de cette réponse dans le suivi voyageur.
     * Défaut métier côté service : {@code true} si non fourni.
     */
    @Schema(description = "Visible à l'auteur du signalement (suivi public)", defaultValue = "true")
    private Boolean publicResponse;
}
