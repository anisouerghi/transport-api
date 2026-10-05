package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    /** Identifiants de notifications souscrites par le voyageur (ex. {@code [1,3,8]}). */
    private List<Integer> notifications;

    /** Langue préférée du voyageur : {@code FR}, {@code AR} ou {@code EN}. */
    private String language;
}
