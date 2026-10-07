package com.transport.reporting.entity;

import com.transport.reporting.converter.IntegerListJsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

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

    /**
     * Notifications souscrites par le voyageur, tableau d'identifiants (ex. {@code [1,3,8]}).
     * Liste libre sans table de référence : la sémantique des IDs est pilotée par le frontend.
     * {@code null} = preference non renseignée.
     *
     * <p>Sérialisé via {@link IntegerListJsonConverter} : MariaDB ne Managed pas
     * {@code CAST(? AS json)}, que produirait {@code @JdbcTypeCode(SqlTypes.JSON)}.
     */
    @Convert(converter = IntegerListJsonConverter.class)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notifications", columnDefinition = "json")
    private List<Integer> notifications;

    /**
     * Langue préférée du voyageur (code ISO 639-1 : {@code FR}, {@code AR}, {@code EN}).
     * {@code null} = preference non renseignée, le client applique alors sa langue par défaut.
     */
    @Column(name = "language", length = 2)
    private String language;

    /**
     * Compte permettant un suivi ultérieur : mot de passe local ou identité Google.
     * Un contact saisi lors d'un dépôt anonyme n'a ni l'un ni l'autre.
     */
    public boolean hasTrackedAccount() {
        return hasText(passwordHash) || hasText(googleSubject);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
