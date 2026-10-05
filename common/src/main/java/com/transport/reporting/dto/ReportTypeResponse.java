package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse type de signalement.
 */
@Data
@Builder
public class ReportTypeResponse {

    private Long reportTypeId;
    private String code;
    /** Libelle localise (Accept-Language). */
    private String label;
    private String labelFr;
    private String labelAr;
    private String labelEn;
    private String description;
    private Integer priority;
    /** Nom Material Symbols (ex. crisis_alert). */
    private String icon;
    private boolean active;
}
