package com.transport.reporting.entity;

import com.transport.reporting.common.enums.Priority;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entite Signalement / reclamation - table report.
 */
@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    /** Identifiant technique auto-incremente du signalement. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    /** Identifiant metier unique (UUID) du signalement. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    /** Reference publique unique pour le suivi du signalement. */
    @Column(name = "reference", nullable = false, unique = true, length = 40)
    private String reference;

    /** Date/heure de creation du signalement. */
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Instant creationDate;

    /** Description detaillee du probleme signale. */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Priorite de traitement (LOW, MEDIUM, HIGH...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 30)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    /** Date/heure de cloture du signalement (null si ouvert). */
    @Column(name = "closure_date")
    private Instant closureDate;

    /** Indique si le signalement est publie (0/1). */
    @Column(name = "publish", nullable = false)
    @Builder.Default
    private Boolean publish = Boolean.FALSE;

    /** Date de publication du signalement. */
    @Column(name = "publish_date")
    private Instant publishDate;

    /** Indique si un email doit etre envoye (0/1). */
    @Column(name = "send_email", nullable = false)
    @Builder.Default
    private Boolean sendEmail = Boolean.FALSE;

    /** Date d'envoi de l'email. */
    @Column(name = "send_email_date")
    private Instant sendEmailDate;

    /** Indique si une reponse publique a ete envoyee (0/1). */
    @Column(name = "public_response", nullable = false)
    @Builder.Default
    private Boolean publicResponse = Boolean.FALSE;

    /** Date de la reponse publique. */
    @Column(name = "public_response_date")
    private Instant publicResponseDate;

    /** Support de transport concerne par le signalement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transport_support_id", nullable = false)
    private TransportSupport transportSupport;

    /** Type / categorie du signalement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_type_id", nullable = false)
    private ReportType reportType;

    /** Voyageur ayant depose le signalement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    /** Statut courant du workflow de traitement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (creationDate == null) {
            creationDate = Instant.now();
        }
    }
}
