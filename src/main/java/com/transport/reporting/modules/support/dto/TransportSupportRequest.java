package com.transport.reporting.modules.support.dto;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransportSupportRequest {

    @NotBlank
    @Size(max = 50)
    private String reference;

    @NotBlank
    @Size(max = 150)
    private String label;

    @Size(max = 500)
    private String qrCodeUrl;

    private QrStatus qrStatus;

    private SupportStatus supportStatus;

    @NotNull
    private Long supportTypeId;
}
