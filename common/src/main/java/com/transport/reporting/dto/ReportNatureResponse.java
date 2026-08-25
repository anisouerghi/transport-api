package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportNatureResponse {

    private Long reportNatureId;
    private String code;
    private String label;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
