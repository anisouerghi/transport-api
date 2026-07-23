package com.transport.reporting.modules.signalement.dto;

import com.transport.reporting.common.enums.TypeSignalement;
import com.transport.reporting.modules.voyageur.dto.VoyageurRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SignalementRequest {

    @NotNull
    private UUID supportUuid;

    @NotNull
    private TypeSignalement type;

    @Size(max = 200)
    private String objet;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @NotNull
    @Valid
    private VoyageurRequest voyageur;
}
