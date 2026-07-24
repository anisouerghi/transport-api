package com.transport.reporting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer / modifier un utilisateur.
 * password est obligatoire a la creation ; optionnel a la modification.
 */
@Data
public class UserRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(min = 8, max = 100)
    private String password;
}
