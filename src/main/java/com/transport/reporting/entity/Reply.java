package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité Réponse agent au voyageur — table reply.
 */
@Entity
@Table(name = "reply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reply_date", nullable = false)
    private Instant replyDate;

    /** Indique si l'e-mail de notification a été envoyé au voyageur. */
    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

    /**
     * Visibilité de cette réponse pour l'auteur du signalement (suivi voyageur).
     * Si false : visible uniquement côté administration.
     */
    @Column(name = "public_response", nullable = false)
    @Builder.Default
    private boolean publicResponse = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

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
