package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse type de support (envoye au frontend).
 */
@Data
@Builder
public class SupportTypeResponse {

    /** Identifiant technique. */
    private Long supportTypeId;

    /** Code metier unique. */
    private String code;

    /** Libelle affiche. */
    private String label;
}
