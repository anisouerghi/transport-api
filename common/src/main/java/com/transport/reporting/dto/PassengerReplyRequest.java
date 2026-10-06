package com.transport.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Message envoyé par le voyageur en réponse à une demande de complément.
 * L'identité et le type de message sont fixés par le serveur.
 */
@Data
@Schema(description = "Réponse voyageur à une demande de complément")
public class PassengerReplyRequest {

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Message du voyageur", example = "Voici le complément demandé.")
    private String message;
}
