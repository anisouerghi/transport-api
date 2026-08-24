package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO reponse signalement.
 */
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
    private Boolean publish;
    private Instant publishDate;
    private Boolean sendEmail;
    private Instant sendEmailDate;
    private Boolean publicResponse;
    private Instant publicResponseDate;
    private TransportSupportResponse transportSupport;
    private String reportTypeCode;
    private String reportTypeLabel;
    private Long natureId;
    private String natureCode;
    private String natureLabel;
    private PassengerResponse passenger;
    private StatusResponse status;
    /** true si au moins une réponse agent est enregistrée. */
    private boolean replied;
    /** Pièces jointes associées (renseignées sur détail / création / suivi). */
    private List<AttachmentResponse> attachments;
}
