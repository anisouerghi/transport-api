package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Entite Piece jointe - table attachment.
 */
@Entity
@Table(name = "attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    /** Identifiant technique auto-incremente de la piece jointe. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    /** Identifiant metier unique (UUID) de la piece jointe. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    /** Nom original du fichier uploade. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Chemin de stockage du fichier sur le serveur. */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** Type MIME / extension du fichier (ex. image/jpeg, application/pdf). */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /** Signalement auquel la piece jointe est rattachee. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
