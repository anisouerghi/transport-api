package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Mise à jour de la priorité d'un signalement (traitement interne admin).
 */
@Data
@Schema(description = "Requête de modification de priorité (agents / administrateurs)")
public class UpdatePriorityRequest {

    @NotNull
    @Schema(description = "Nouvelle priorité", example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private Priority priority;
}
