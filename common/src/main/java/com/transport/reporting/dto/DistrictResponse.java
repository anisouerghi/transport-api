package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse district (envoye au frontend).
 */
@Data
@Builder
public class DistrictResponse {

    /** Identifiant technique. */
    private Long districtId;

    /** Code metier unique. */
    private String codeDistrict;

    /** Libelle descriptif. */
    private String libelleDistrict;

    /** Etat du district. */
    private Integer etat;
}
