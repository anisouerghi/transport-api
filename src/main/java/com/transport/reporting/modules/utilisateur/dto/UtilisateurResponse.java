package com.transport.reporting.modules.utilisateur.dto;

import com.transport.reporting.common.enums.RoleUtilisateur;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UtilisateurResponse {

    private Long id;
    private String login;
    private String nom;
    private String email;
    private RoleUtilisateur role;
    private boolean actif;
}
