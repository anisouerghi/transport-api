package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite Type de signalement - table report_type.
 */
@Entity
@Table(name = "report_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportType {

    /** Identifiant technique auto-incremente du type de signalement. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_type_id")
    private Long reportTypeId;

    /** Code metier unique du type (ex. INCIDENT, SUGGESTION). */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Libelle historique (miroir FR = label_fr). */
    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "label_fr", length = 150)
    private String labelFr;

    @Column(name = "label_ar", length = 150)
    private String labelAr;

    @Column(name = "label_en", length = 150)
    private String labelEn;

    /** Description detaillee du type de signalement. */
    @Column(name = "description", length = 500)
    private String description;

    /** Indique si le type est actif (visible / utilisable). */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
