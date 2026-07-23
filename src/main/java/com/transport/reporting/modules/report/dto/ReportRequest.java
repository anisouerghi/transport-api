package com.transport.reporting.modules.report.dto;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.modules.passenger.dto.PassengerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ReportRequest {

    @NotNull
    private UUID supportUuid;

    @NotNull
    private Long reportTypeId;

    private Priority priority;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @NotNull
    @Valid
    private PassengerRequest passenger;
}
