package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Réponse renvoyée lorsque le login est valide mais qu'un OTP e-mail est requis.
 * Ne contient jamais le code OTP.
 */
@Getter
@Setter
@Builder
public class PassengerOtpPendingResponse {

    private String otpTransactionId;
    private int expiresInSeconds;
    private int resendDelaySeconds;
    /** E-mail masqué pour affichage (ex. v***@example.com). */
    private String maskedEmail;
    /** {@code false} si le SMTP a échoué — l'écran OTP reste affiché, renvoi possible. */
    private boolean emailSent = true;
}
