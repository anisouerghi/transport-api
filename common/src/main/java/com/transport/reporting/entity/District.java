package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite District - table district.
 */
@Entity
@Table(name = "district", indexes = {
        @Index(name = "idx_district_code", columnList = "code_district"),
        @Index(name = "idx_district_etat", columnList = "etat")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    /** Identifiant technique auto-incremente du district. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "district_id")
    private Long districtId;

    /** Code metier unique du district. */
    @Column(name = "code_district", nullable = false, unique = true, length = 10)
    private String codeDistrict;

    /** Libelle descriptif du district. */
    @Column(name = "libelle_district", nullable = false, length = 45)
    private String libelleDistrict;

    /** Etat du district (0 = inactif, 1 = actif). */
    @Column(name = "etat", nullable = false)
    @Builder.Default
    private Integer etat = 1;
}
