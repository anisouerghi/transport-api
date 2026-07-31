package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO de réponse décrivant une pièce jointe pour les clients Angular (admin / public).
 * <p>
 * {@code fileSize} est calculé à la volée depuis le disque (non stocké en base).
 * {@code image} indique si le MIME permet un aperçu miniature.
 */
@Data
@Builder
public class AttachmentResponse {

    private Long attachmentId;
    private UUID uuid;
    /** Nom d'origine affiché à l'utilisateur. */
    private String fileName;
    /** Type MIME validé (ex. {@code image/jpeg}, {@code application/pdf}). */
    private String fileType;
    /** Taille en octets si le fichier est encore présent sur disque. */
    private Long fileSize;
    private Long reportId;
    /** {@code true} lorsque le fichier est une image visualisable inline. */
    private boolean image;
}
