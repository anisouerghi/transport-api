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

    /** Libelle historique (miroir FR = label_fr). */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "label_fr", length = 100)
    private String labelFr;

    @Column(name = "label_ar", length = 100)
    private String labelAr;

    @Column(name = "label_en", length = 100)
    private String labelEn;

    /** Ordre d'affichage dans le workflow / listes. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
