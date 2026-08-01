package com.transport.reporting.dto;

import lombok.Data;

/**
 * Critères de recherche multicritère des voyageurs.
 */
@Data
public class PassengerCriteria {

    private String name;
    private String email;
    private String phoneNumber;
    /** null = tous, true = actifs, false = désactivés. */
    private Boolean active;
}
