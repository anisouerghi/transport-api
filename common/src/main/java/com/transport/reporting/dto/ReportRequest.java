package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO requête de création d'un signalement (API publique).
 * La priorité n'est pas fournie par le voyageur : elle est définie côté métier
 * (valeur par défaut à la création, puis ajustée par les agents).
 */
@Data
@Schema(description = "Création d'un signalement voyageur (sans priorité)")
public class ReportRequest {

    private UUID supportUuid;

    private Long reportTypeId;

    private Boolean publish;

    private Instant publishDate;

    private Boolean sendEmail;

    private Instant sendEmailDate;

    private Boolean publicResponse;

    private Instant publicResponseDate;

    @NotBlank
    @Size(max = 5000)
    private String description;

    private PassengerRequest passenger;

    /**
     * Token Cloudflare Turnstile (optionnel côté schéma ; obligatoire si Turnstile est activé côté serveur).
     * Jamais persisté — utilisé uniquement pour la validation anti-bot à la création.
     */
    @Size(max = 2048)
    @Schema(description = "Token Turnstile renvoyé par le widget Cloudflare")
    private String turnstileToken;
}
