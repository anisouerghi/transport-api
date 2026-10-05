package com.transport.reporting.dto;

import com.transport.reporting.common.enums.PassengerLanguage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO requete voyageur.
 */
@Data
public class PassengerRequest {

    @Size(max = 150)
    private String name;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phoneNumber;

    /** Identifiants de notifications souscrites (ex. {@code [1,3,8]}). */
    @Size(max = PassengerLanguage.MAX_NOTIFICATIONS)
    private List<@NotNull @Positive Integer> notifications;

    /** Langue préférée du voyageur : {@code FR}, {@code AR} ou {@code EN}. */
    @Pattern(regexp = PassengerLanguage.REGEX, message = PassengerLanguage.INVALID_MESSAGE)
    private String language;
}
