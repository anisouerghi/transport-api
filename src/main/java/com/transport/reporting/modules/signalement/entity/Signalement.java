package com.transport.reporting.modules.signalement.entity;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.common.enums.TypeSignalement;
import com.transport.reporting.modules.support.entity.Support;
import com.transport.reporting.modules.voyageur.entity.Voyageur;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Réclamation ou incident déclaré par un voyageur.
 */
@Entity
@Table(name = "signalement", indexes = {
        @Index(name = "idx_signalement_reference", columnList = "reference", unique = true),
        @Index(name = "idx_signalement_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutSignalement statut = StatutSignalement.NOUVEAU;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeSignalement type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_id", nullable = false)
    private Support support;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyageur_id", nullable = false)
    private Voyageur voyageur;

    /** Champs métier utiles pour l'administration (évolutifs). */
    @Column(length = 200)
    private String objet;

    @Column(length = 100)
    private String serviceAffecte;

    @Column(columnDefinition = "TEXT")
    private String reponse;

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
