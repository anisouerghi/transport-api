package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
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

    @Size(max = 500)
    private String description;
}
