package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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
     * Mot de passe BCrypt. {@code null} = contact anonyme ou compte Google uniquement.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * Identifiant Google stable (claim {@code sub} OIDC). Clé d'association du compte OAuth.
     */
    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    /** Fournisseur d'authentification ({@code LOCAL} ou {@code GOOGLE}). */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20, nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /** URL photo de profil (ex. claim OIDC {@code picture} Google). */
    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    /** Dernière IP HTTP connue (serveur). */
    @Column(name = "last_ip", length = 64)
    private String lastIp;

    /** Dernier User-Agent brut (tronqué). */
    @Column(name = "last_user_agent", length = 512)
    private String lastUserAgent;

    /** Navigateur détecté (Chrome, Safari, …). */
    @Column(name = "last_browser", length = 50)
    private String lastBrowser;

    /** Latitude GPS optionnelle (navigateur). */
    @Column(name = "latitude")
    private Double latitude;

    /** Longitude GPS optionnelle (navigateur). */
    @Column(name = "longitude")
    private Double longitude;

    /** Précision GPS en mètres (optionnelle). */
    @Column(name = "gps_accuracy")
    private Double gpsAccuracy;

    /** Horodatage de la dernière position GPS reçue. */
    @Column(name = "gps_captured_at")
    private Instant gpsCapturedAt;

    /** Dernière authentification réussie (login / OTP / Google). */
    @Column(name = "last_auth_at")
    private Instant lastAuthAt;
}
