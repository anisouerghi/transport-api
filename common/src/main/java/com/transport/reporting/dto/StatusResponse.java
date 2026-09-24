package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse statut.
 */
@Data
@Builder
public class StatusResponse {

    private Long statusId;
    private String code;
    private String label;
    private String labelFr;
    private String labelAr;
    private String labelEn;
    private Integer displayOrder;
}
