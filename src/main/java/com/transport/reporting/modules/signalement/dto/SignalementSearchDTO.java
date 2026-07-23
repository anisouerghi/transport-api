package com.transport.reporting.modules.signalement.dto;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.common.enums.TypeSignalement;
import lombok.Data;

@Data
public class SignalementSearchDTO {

    private String reference;
    private TypeSignalement type;
    private StatutSignalement statut;
    private String serviceAffecte;
    private Long supportId;
}
