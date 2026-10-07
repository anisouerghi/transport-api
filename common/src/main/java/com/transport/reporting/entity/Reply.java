package com.transport.reporting.entity;

import com.transport.reporting.common.enums.ReplyAuthorType;
import com.transport.reporting.common.enums.ReplyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Message de la conversation d'un signalement — table reply.
 * Le texte initial du signalement reste sur {@link Report#getDescription()}.
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

    /**
     * Rôle du message. Les lignes antérieures à la conversation sont lues
     * comme {@link ReplyType#RESPONSE} lorsque la colonne est encore vide.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reply_type", nullable = false, length = 40)
    @Builder.Default
    private ReplyType replyType = ReplyType.RESPONSE;

    /**
     * Auteur fixé par le serveur. Les lignes historiques sont lues comme
     * {@link ReplyAuthorType#AGENT} lorsque la colonne est encore vide.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 20)
    @Builder.Default
    private ReplyAuthorType authorType = ReplyAuthorType.AGENT;

    /** Voyageur auteur, uniquement pour une réponse de complément. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    /**
     * Type effectif, y compris pour une ligne chargée avant le remplissage
     * des colonnes de conversation.
     */
    public ReplyType effectiveType() {
        return replyType == null ? ReplyType.RESPONSE : replyType;
    }

    /**
     * Auteur effectif, y compris pour une ligne historique sans auteur stocké.
     */
    public ReplyAuthorType effectiveAuthor() {
        return authorType == null ? ReplyAuthorType.AGENT : authorType;
    }

    /**
     * Visible du voyageur : réponse publique et différente d'une note interne.
     * Une note interne reste masquée même si l'ancien indicateur public était vrai.
     */
    public boolean isVisibleToPassenger() {
        return publicResponse && effectiveType() != ReplyType.INTERNAL_NOTE;
    }

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
