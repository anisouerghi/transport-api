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
    private String label;
    private String description;
    private boolean active;
}
