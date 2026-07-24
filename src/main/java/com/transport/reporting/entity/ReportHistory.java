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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_status_id")
    private Status oldStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_status_id", nullable = false)
    private Status newStatus;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "action_date", nullable = false)
    private Instant actionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

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
