package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite Type de support (bus, metro, etc.) - table support_type.
 */
@Entity
@Table(name = "support_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportType {

    /** Identifiant technique auto-incremente du type de support. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_type_id")
    private Long supportTypeId;

    /** Code metier unique du type (ex. BUS, METRO). */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Libelle affiche du type de support. */
    @Column(name = "label", nullable = false, length = 150)
    private String label;
}
