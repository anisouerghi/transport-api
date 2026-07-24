package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse voyageur.
 */
@Data
@Builder
public class PassengerResponse {

    private Long passengerId;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean emailVerified;
}
