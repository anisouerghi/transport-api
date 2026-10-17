package com.transport.reporting.dto;

import com.transport.reporting.common.enums.PassengerLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Mise à jour partielle des préférences voyageur.
 * Champ absent = inchangé.
 */
@Data
public class PassengerPreferenceUpdateRequest {

    @Schema(description = "Langue préférée : FR, AR ou EN.", example = "AR", allowableValues = {"FR", "AR", "EN"})
    @Pattern(regexp = PassengerLanguage.REGEX, message = PassengerLanguage.INVALID_MESSAGE)
    private String language;

    @Schema(description = "Canaux souscrits (1 plateforme, 2 email, 3 SMS, 4 WhatsApp). [] = aucun.",
            example = "[1,2]")
    @Size(max = PassengerLanguage.MAX_NOTIFICATIONS)
    private List<@NotNull @Positive Integer> notifications;
}
