package com.transport.reporting.dto;

import lombok.Data;

/**
 * Criteres de recherche multicritere pour SupportType.
 * Chaque champ est optionnel : seuls les champs renseignes sont appliques dans la Specification.
 */
@Data
public class SupportTypeCriteria {

    /** Filtre partiel sur le code (LIKE, insensible a la casse). Ex. "BUS". */
    private String code;

    /** Filtre partiel sur le libelle (LIKE, insensible a la casse). */
    private String label;
}
