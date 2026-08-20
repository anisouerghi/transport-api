package com.transport.reporting.dto;

import lombok.Data;

/**
 * Criteres de recherche multicritere pour District.
 * Chaque champ est optionnel : seuls les champs renseignes sont appliques dans la Specification.
 */
@Data
public class DistrictCriteria {

    /** Filtre partiel sur le code (LIKE, insensible a la casse). */
    private String codeDistrict;

    /** Filtre partiel sur le libelle (LIKE, insensible a la casse). */
    private String libelleDistrict;

    /** Filtre sur l'etat (0 ou 1). */
    private Integer etat;
}
