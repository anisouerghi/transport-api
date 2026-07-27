package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entite Reponse au voyageur - table reply.
 */
@Entity
@Table(name = "reply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reply {

    /** Identifiant technique auto-incremente de la reponse. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    /** Identifiant metier unique (UUID) de la reponse. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    /** Contenu du message envoye au voyageur. */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Date/heure d'emission de la reponse. */
    @Column(name = "reply_date", nullable = false)
    private Instant replyDate;

    /** Indique si l'e-mail de notification a ete envoye au voyageur. */
    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

    /** Signalement concerne par cette reponse. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    /** Agent / administrateur auteur de la reponse (optionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (replyDate == null) {
            replyDate = Instant.now();
        }
    }
}
