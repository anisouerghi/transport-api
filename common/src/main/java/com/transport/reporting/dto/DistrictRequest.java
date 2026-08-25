package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer / modifier un district.
 */
@Data
public class DistrictRequest {

    /** Code metier unique du district. */
    @NotBlank
    @Size(max = 10)
    private String codeDistrict;

    /** Libelle descriptif du district. */
    @NotBlank
    @Size(max = 45)
    private String libelleDistrict;

    /** Etat du district (0 = inactif, 1 = actif). */
    private Integer etat;
}
