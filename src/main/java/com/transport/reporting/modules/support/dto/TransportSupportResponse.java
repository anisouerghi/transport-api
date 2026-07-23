package com.transport.reporting.modules.support.dto;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransportSupportResponse {

    private Long transportSupportId;
    private UUID uuid;
    private String reference;
    private String label;
    private String qrCodeUrl;
    private Instant qrDateCreation;
    private Instant qrDateImpression;
    private QrStatus qrStatus;
    private SupportStatus supportStatus;
    private String supportTypeCode;
    private String supportTypeLabel;
}
