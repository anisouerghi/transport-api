package com.transport.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO réponse d'une réponse agent sur un signalement.
 */
@Data
@Builder
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
}
