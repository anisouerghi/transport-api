package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Répartition des signalements par statut (KPI tableau de bord).
 * Le libellé est résolu selon la locale de la requête (Accept-Language).
 */
@Data
@Builder
public class ReportStatusCountResponse {

    private Long statusId;
    private String code;
    private String label;
    private long count;
}
