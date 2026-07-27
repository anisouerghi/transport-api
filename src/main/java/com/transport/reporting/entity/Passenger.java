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

    /** Identifiant technique auto-incremente du voyageur. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long passengerId;

    /** Nom du voyageur (declarant du signalement). */
    @Column(name = "name", length = 150)
    private String name;

    /** Adresse e-mail du voyageur (suivi / notifications). */
    @Column(name = "email", length = 255)
    private String email;

    /** Numero de telephone du voyageur. */
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    /** Indique si l'e-mail du voyageur a ete verifie. */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;
}
