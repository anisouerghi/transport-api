package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReportResponse {

    private Long reportId;
    private UUID uuid;
    private String reference;
    private Instant creationDate;
    private String description;
    private Priority priority;
    private Instant closureDate;
    private TransportSupportResponse transportSupport;
    private String reportTypeCode;
    private String reportTypeLabel;
    private PassengerResponse passenger;
    private StatusResponse status;
}
