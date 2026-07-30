package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer une reponse a un signalement.
 */
@Data
public class ReplyRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;

    @NotNull
    private Long userId;

    /** Nouveau statut optionnel du signalement. */
    private Long statusId;

    /** Si true, marque emailSent (envoi reel a implementer plus tard). */
    private Boolean sendEmail;

    /** Reserve (non persiste pour le moment). */
    private Boolean publish;

    /** Reserve (non persiste pour le moment). */
    private Boolean publicResponse;

}
