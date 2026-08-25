package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour la mise à jour du mot de passe de l'utilisateur connecté.
 */
@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String oldPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le nouveau mot de passe doit contenir entre 8 et 100 caractères")
    private String newPassword;
}
