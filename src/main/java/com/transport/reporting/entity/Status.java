package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite Statut du workflow de signalement - table report_status.
 */
@Entity
@Table(name = "report_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Status {

    /** Identifiant technique auto-incremente du statut. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    /** Code metier unique du statut (ex. NEW, IN_PROGRESS, CLOSED). */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Libelle affiche du statut. */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** Ordre d'affichage dans le workflow / listes. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
