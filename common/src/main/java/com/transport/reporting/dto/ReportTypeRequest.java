package com.transport.reporting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer / modifier un type de signalement.
 */
@Data
public class ReportTypeRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String label;

    @Size(max = 150)
    private String labelAr;

    @Size(max = 150)
    private String labelEn;

    @Size(max = 500)
    private String description;

    /** Ordre d'affichage : 1 = premier. */
    @NotNull
    @Min(1)
    @Max(9999)
    private Integer priority;

    /** Nom d'icône Material Symbols, sans fichier. */
    @Size(max = 80)
    private String icon;
}
