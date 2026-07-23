package com.transport.reporting.modules.voyageur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Déclarant d'un signalement (voyageur).
 */
@Entity
@Table(name = "voyageur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voyageur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(length = 100)
    private String nom;

    /** Email facultatif. */
    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String telephone;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
