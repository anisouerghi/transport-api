package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PassengerResponse {

    private Long passengerId;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean emailVerified;
    private boolean active;
    /**
     * True si aucune identité renseignée (nom / e-mail / téléphone absents).
     * Correspond au type « Voyageur anonyme » côté administration.
     */
    private boolean anonymous;
}
