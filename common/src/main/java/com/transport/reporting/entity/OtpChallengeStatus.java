package com.transport.reporting.entity;

/**
 * Statut d'une transaction OTP voyageur.
 */
public enum OtpChallengeStatus {
    PENDING,
    VERIFIED,
    EXPIRED,
    LOCKED,
    CANCELLED
}
