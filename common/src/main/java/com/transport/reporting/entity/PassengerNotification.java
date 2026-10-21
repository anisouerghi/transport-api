package com.transport.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Notification in-app destinée à un compte voyageur authentifié.
 * Persistée indépendamment du polling Angular (cloche).
 */
@Entity
@Table(
        name = "passenger_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_passenger_notification_dedup",
                columnNames = {"passenger_id", "dedup_key"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerNotification {

    public static final String EVENT_STATUS_PUBLIC_CHANGED = "STATUS_PUBLIC_CHANGED";
    public static final String EVENT_COMPLEMENT_REQUEST = "COMPLEMENT_REQUEST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    /** Type métier : {@link #EVENT_STATUS_PUBLIC_CHANGED} ou {@link #EVENT_COMPLEMENT_REQUEST}. */
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    /** Snapshot du libellé public (colonne {@code report_status.statut_public}). */
    @Column(name = "public_status", length = 150)
    private String publicStatus;

    /** Texte affiché au voyageur (sans données internes agent). */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /**
     * Clé d'unicité anti-doublon, ex. {@code STATUS:12:En cours de traitement}
     * ou {@code COMPLEMENT:reply:50}.
     */
    @Column(name = "dedup_key", nullable = false, length = 180)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** {@code null} = non lu. */
    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
