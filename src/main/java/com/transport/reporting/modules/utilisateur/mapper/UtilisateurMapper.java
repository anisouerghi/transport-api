package com.transport.reporting.modules.utilisateur.mapper;

import com.transport.reporting.modules.utilisateur.dto.UtilisateurRequest;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurResponse;
import com.transport.reporting.modules.utilisateur.entity.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public Utilisateur toEntity(UtilisateurRequest request, String encodedPassword) {
        return Utilisateur.builder()
                .login(request.getLogin())
                .password(encodedPassword)
                .nom(request.getNom())
                .email(request.getEmail())
                .role(request.getRole())
                .actif(request.getActif() == null || request.getActif())
                .build();
    }

    public UtilisateurResponse toResponse(Utilisateur utilisateur) {
        return UtilisateurResponse.builder()
                .id(utilisateur.getId())
                .login(utilisateur.getLogin())
                .nom(utilisateur.getNom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .actif(utilisateur.isActif())
                .build();
    }
}
