package com.transport.reporting.modules.signalement.dto;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.common.enums.TypeSignalement;
import com.transport.reporting.modules.support.dto.SupportResponse;
import com.transport.reporting.modules.voyageur.dto.VoyageurResponse;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SignalementResponse {

    private Long id;
    private String reference;
    private String description;
    private Instant dateCreation;
    private StatutSignalement statut;
    private TypeSignalement type;
    private String objet;
    private String serviceAffecte;
    private String reponse;
    private SupportResponse support;
    private VoyageurResponse voyageur;
}
