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

    /** Libelle affiche du type de signalement. */
    @Column(name = "label", nullable = false, length = 150)
    private String label;

    /** Description detaillee du type de signalement. */
    @Column(name = "description", length = 500)
    private String description;
}
