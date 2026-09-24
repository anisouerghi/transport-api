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

    /** Libelle affiche (localise selon Accept-Language). */
    private String label;

    /** Libelle francais (edition admin). */
    private String labelFr;

    /** Libelle arabe (edition admin). */
    private String labelAr;

    /** Libelle anglais (edition admin). */
    private String labelEn;
}
