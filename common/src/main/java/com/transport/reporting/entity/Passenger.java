package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite Voyageur (declarant) - table passenger.
 */
@Entity
@Table(name = "passenger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long passengerId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /** Compte voyageur actif (peut déposer / être contacté). */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Mot de passe BCrypt. {@code null} = contact anonyme sans compte.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
}
