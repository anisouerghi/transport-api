package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer / modifier un type de support.
 */
@Data
public class SupportTypeRequest {

    /** Code metier unique (ex. BUS, METRO, TRAIN). */
    @NotBlank
    @Size(max = 50)
    private String code;

    /** Libelle affiche (localise selon Accept-Language). */
    @NotBlank
    @Size(max = 150)
    private String label;

    /** Libelle arabe (edition admin). */
    @Size(max = 150)
    private String labelAr;

    /** Libelle anglais (edition admin). */
    @Size(max = 150)
    private String labelEn;
}
