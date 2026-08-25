package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entite Historique des changements de statut - table report_history.
 */
@Entity
@Table(name = "report_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory {

    /** Identifiant technique auto-incremente de l'entree d'historique. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    /** Statut precedent du signalement (null a la creation). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_status_id")
    private Status oldStatus;

    /** Nouveau statut apres le changement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_status_id", nullable = false)
    private Status newStatus;

    /** Commentaire / motif du changement de statut. */
    @Column(name = "comments", length = 1000)
    private String comments;

    /** Date/heure de l'action de changement. */
    @Column(name = "action_date", nullable = false)
    private Instant actionDate;

    /** Signalement concerne par cet historique. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    /** Utilisateur ayant effectue le changement (optionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    @PrePersist
    public void prePersist() {
        if (actionDate == null) {
            actionDate = Instant.now();
        }
    }
}
