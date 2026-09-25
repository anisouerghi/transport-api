package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requête pour créer / modifier un statut.
 */
@Data
public class StatusRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères")
    private String label;

    @Size(max = 100)
    private String labelAr;

    @Size(max = 100)
    private String labelEn;

    @NotNull(message = "L'ordre d'affichage est obligatoire")
    private Integer displayOrder;
}