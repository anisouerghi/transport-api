package com.transport.reporting.dto;

import com.transport.reporting.common.enums.SatisfactionLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Avis du voyageur sur la réponse apportée à un signalement clôturé.
 * Le serveur fixe le signalement, l'auteur et l'unicité.
 */
@Data
@Schema(description = "Évaluation de la réponse TRANSTU")
public class PublicSatisfactionRequest {

    @NotNull
    @Schema(description = "SATISFAIT, MOYENNEMENT_SATISFAIT ou NON_SATISFAIT", example = "SATISFAIT")
    private SatisfactionLevel level;

    @Size(max = 1000)
    @Schema(description = "Commentaire facultatif, 1000 caractères au plus")
    private String comment;
}
