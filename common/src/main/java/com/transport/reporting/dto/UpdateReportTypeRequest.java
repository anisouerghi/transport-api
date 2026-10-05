package com.transport.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Mise à jour du type ({@code ReportType}) d'un signalement — natures voyageur.
 * {@code reportTypeId} null = retirer le type (Non définie).
 */
@Data
@Schema(description = "Modification du type de signalement (ReportType)")
public class UpdateReportTypeRequest {

    /** Identifiant du type actif, ou null pour effacer. */
    @Schema(description = "Identifiant report_type, null = non définie")
    private Long reportTypeId;
}
