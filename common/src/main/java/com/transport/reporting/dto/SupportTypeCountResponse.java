package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Répartition des supports de transport par type de support (statistique).
 * Le libellé est résolu selon la locale de la requête (Accept-Language) ;
 * {@code labelAr} et {@code labelEn} exposent toujours les libellés de référence.
 */
@Data
@Builder
public class SupportTypeCountResponse {

    private Long supportTypeId;
    private String code;
    private String label;
    private String labelAr;
    private String labelEn;
    private long count;
}
