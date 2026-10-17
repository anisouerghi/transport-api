package com.transport.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Préférences persistées d'un compte voyageur (une ligne max par passager).
 */
@Entity
@Table(
        name = "passenger_preference",
        uniqueConstraints = @UniqueConstraint(name = "uk_passenger_preference_passenger", columnNames = "passenger_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false, unique = true)
    private Passenger passenger;

    /** Code canonique {@code FR}, {@code AR} ou {@code EN}. */
    @Column(name = "language", nullable = false, length = 2)
    private String language;

    @Builder.Default
    @Column(name = "notify_platform", nullable = false)
    private boolean notifyPlatform = true;

    @Builder.Default
    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = true;

    @Builder.Default
    @Column(name = "notify_sms", nullable = false)
    private boolean notifySms = false;

    @Builder.Default
    @Column(name = "notify_whatsapp", nullable = false)
    private boolean notifyWhatsapp = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
