package com.transport.reporting.modules.signalement.mapper;

import com.transport.reporting.modules.signalement.dto.SignalementResponse;
import com.transport.reporting.modules.signalement.entity.Signalement;
import com.transport.reporting.modules.support.mapper.SupportMapper;
import com.transport.reporting.modules.voyageur.mapper.VoyageurMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignalementMapper {

    private final SupportMapper supportMapper;
    private final VoyageurMapper voyageurMapper;

    public SignalementResponse toResponse(Signalement signalement) {
        return SignalementResponse.builder()
                .id(signalement.getId())
                .reference(signalement.getReference())
                .description(signalement.getDescription())
                .dateCreation(signalement.getDateCreation())
                .statut(signalement.getStatut())
                .type(signalement.getType())
                .objet(signalement.getObjet())
                .serviceAffecte(signalement.getServiceAffecte())
                .reponse(signalement.getReponse())
                .support(supportMapper.toResponse(signalement.getSupport()))
                .voyageur(voyageurMapper.toResponse(signalement.getVoyageur()))
                .build();
    }
}
