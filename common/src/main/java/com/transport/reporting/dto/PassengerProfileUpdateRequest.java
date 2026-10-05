package com.transport.reporting.dto;

import com.transport.reporting.common.enums.PassengerLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Mise à jour du profil voyageur connecté ({@code PUT /api/public/auth/me}).
 * Tous les champs sont optionnels : seuls les champs transmis sont modifiés.
 */
@Data
public class PassengerProfileUpdateRequest {

    @Size(max = 150)
    private String name;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phoneNumber;

    @Schema(description = "Identifiants de notifications souscrites (ex. [1,3,8]). "
            + "Ignoré si absent ; [] désactive toutes les notifications.",
            example = "[1,3,8]")
    @Size(max = PassengerLanguage.MAX_NOTIFICATIONS)
    private List<@NotNull @Positive Integer> notifications;

    @Schema(description = "Langue préférée du voyageur : FR, AR ou EN. Ignorée si absente.",
            example = "FR", allowableValues = {"FR", "AR", "EN"})
    @Pattern(regexp = PassengerLanguage.REGEX, message = PassengerLanguage.INVALID_MESSAGE)
    private String language;

    private String currentPassword;

    private String password;
}
