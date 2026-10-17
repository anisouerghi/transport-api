package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerPreferenceResponse {

    private Long preferenceId;
    private Long passengerId;
    /** Code canonique {@code FR}, {@code AR} ou {@code EN}. */
    private String language;
    /** Identifiants des canaux : 1 plateforme, 2 email, 3 SMS, 4 WhatsApp. */
    private List<Integer> notifications;
    private boolean notifyPlatform;
    private boolean notifyEmail;
    private boolean notifySms;
    private boolean notifyWhatsapp;
    private Instant createdAt;
    private Instant updatedAt;
}
