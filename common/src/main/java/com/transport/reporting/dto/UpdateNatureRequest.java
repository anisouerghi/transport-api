package com.transport.reporting.dto;

import lombok.Data;

/**
 * Affectation / modification de la nature d'un signalement.
 * {@code reportNatureId} null = retirer la classification (Non classé).
 */
@Data
public class UpdateNatureRequest {

    /** Identifiant de la nature active, ou null pour déclasser. */
    private Long reportNatureId;
}
