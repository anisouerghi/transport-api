package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Élément de la boîte de notifications voyageur (données publiques uniquement). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerNotificationItemResponse {

    private Long id;
    private String reportReference;
    private UUID reportUuid;
    private String message;
    private String publicStatus;
    private String eventType;
    private Instant createdAt;
    private boolean read;
}
