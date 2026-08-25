package com.transport.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO réponse d'une réponse agent sur un signalement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse agent")
public class ReplyResponse {

    private Long replyId;
    private String message;
    private Instant replyDate;
    private boolean emailSent;
    /** Visible dans le suivi voyageur. */
    private boolean publicResponse;
    private Long reportId;
    private Long userId;
    /** Flag publication accueil (niveau signalement, legacy). */
    private Boolean publish;

    /** true si un envoi e-mail a été demandé. */
    private Boolean emailRequested;
    /** Résultat lisible de l'envoi e-mail (succès ou cause d'échec). */
    private String emailMessage;
    /** Code d'erreur e-mail optionnel. */
    private String emailErrorCode;
}
