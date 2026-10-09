package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Répartition des signalements par type de support (KPI tableau de bord).
 * Le libellé est résolu selon la locale de la requête (Accept-Language).
 */
@Data
@Builder
public class ReportSupportTypeCountResponse {

    private Long supportTypeId;
    private String code;
    private String label;
    private long count;
}
