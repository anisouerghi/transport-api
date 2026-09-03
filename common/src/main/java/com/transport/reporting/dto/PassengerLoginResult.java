package com.transport.reporting.dto;

import lombok.Getter;

/**
 * Résultat du login voyageur : JWT direct ou attente OTP.
 */
@Getter
public class PassengerLoginResult {

    private final PassengerAuthResponse authResponse;
    private final PassengerOtpPendingResponse otpPending;

    private PassengerLoginResult(PassengerAuthResponse authResponse, PassengerOtpPendingResponse otpPending) {
        this.authResponse = authResponse;
        this.otpPending = otpPending;
    }

    public static PassengerLoginResult jwt(PassengerAuthResponse authResponse) {
        return new PassengerLoginResult(authResponse, null);
    }

    public static PassengerLoginResult otpRequired(PassengerOtpPendingResponse otpPending) {
        return new PassengerLoginResult(null, otpPending);
    }

    public boolean isOtpRequired() {
        return otpPending != null;
    }
}
