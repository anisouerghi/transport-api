package com.transport.reporting.dto;

import com.transport.reporting.common.enums.SupportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO requete pour creer / modifier un support de transport.
 * <p>
 * Important : uuid, qrCodeUrl, qrCodePath, qrStatus et qrDateCreation
 * ne doivent PAS etre envoyes par le frontend — ils sont geres par le backend.
 */
@Data
public class TransportSupportRequest {

    /** Reference metier unique (ex. BUS-L12-4521). */
    @NotBlank
    @Size(max = 50)
    private String reference;

    /** Libelle descriptif du support. */
    @NotBlank
    @Size(max = 150)
    private String label;

    /** Statut operationnel (ACTIVE par defaut a la creation si null). */
    private SupportStatus supportStatus;

    /** Identifiant du type de support (obligatoire). */
    @NotNull
    private Long supportTypeId;

    /**
     * Version pour verrouillage optimiste.
     * A renvoyer telle quelle lors d'une modification (PUT).
     */
    private Long version;
}
