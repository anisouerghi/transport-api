package com.transport.reporting.modules.signalement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AffectationRequest {

    @NotBlank
    @Size(max = 100)
    private String serviceAffecte;
}
