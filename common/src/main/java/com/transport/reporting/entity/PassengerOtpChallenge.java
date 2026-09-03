package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Transaction OTP temporaire liée à un voyageur (login e-mail/mot de passe).
 * Le code est stocké hashé ; jamais en clair en base.
 */
@Entity
@Table(name = "passenger_otp_challenge", indexes = {
        @Index(name = "idx_otp_challenge_passenger", columnList = "passenger_id"),
        @Index(name = "idx_otp_challenge_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "send_count", nullable = false)
    @Builder.Default
    private int sendCount = 1;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OtpChallengeStatus status = OtpChallengeStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastSentAt == null) {
            lastSentAt = now;
        }
    }
}
