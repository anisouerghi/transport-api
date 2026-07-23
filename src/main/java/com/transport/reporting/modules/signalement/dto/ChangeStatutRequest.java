package com.transport.reporting.modules.signalement.dto;

import com.transport.reporting.common.enums.StatutSignalement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeStatutRequest {

    @NotNull
    private StatutSignalement statut;

    @Size(max = 500)
    private String commentaire;
}
