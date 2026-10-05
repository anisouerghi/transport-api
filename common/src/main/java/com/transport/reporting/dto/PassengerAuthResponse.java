package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerAuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresInMs;
    private Long passengerId;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private String authProvider;

    /**
     * Identifiants de notifications souscrites (ex. {@code [1,3,8]}).
     * Absent de la réponse tant que le voyageur n'a pas défini la préférence.
     */
    private List<Integer> notifications;

    /** Langue préférée du voyageur : {@code FR}, {@code AR} ou {@code EN}. */
    private String language;
}
