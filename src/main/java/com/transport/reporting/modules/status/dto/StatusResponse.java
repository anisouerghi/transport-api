package com.transport.reporting.modules.status.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusResponse {

    private Long statusId;
    private String code;
    private String label;
    private Integer displayOrder;
}
