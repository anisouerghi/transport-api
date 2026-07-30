package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO reponse d'une reponse agent sur un signalement.
 */
@Data
@Builder
public class ReplyResponse {

    private Long replyId;
    private String message;
    private Instant replyDate;
    private boolean emailSent;
    private Long reportId;
    private Long userId;
}
