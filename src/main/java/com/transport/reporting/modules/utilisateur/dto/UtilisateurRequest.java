package com.transport.reporting.modules.utilisateur.dto;

import com.transport.reporting.common.enums.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UtilisateurRequest {

    @NotBlank
    @Size(max = 100)
    private String login;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String nom;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotNull
    private RoleUtilisateur role;

    private Boolean actif;
}
