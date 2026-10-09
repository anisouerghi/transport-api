package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Répartition des signalements par type (KPI tableau de bord).
 * Le libellé est résolu selon la locale de la requête (Accept-Language).
 */
@Data
@Builder
public class ReportTypeCountResponse {

    private Long reportTypeId;
    private String code;
    private String label;
    private long count;
}
