package com.transport.reporting.entity;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entite Support de transport (porteur du QR Code) - table transport_support.
 */
@Entity
@Table(name = "transport_support", indexes = {
        @Index(name = "idx_transport_support_reference", columnList = "reference"),
        @Index(name = "idx_transport_support_uuid", columnList = "uuid"),
        @Index(name = "idx_transport_support_support_status", columnList = "support_status"),
        @Index(name = "idx_transport_support_qr_status", columnList = "qr_status")
})
public class TransportSupport {

    /** Identifiant technique auto-incremente du support. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transport_support_id")
    private Long transportSupportId;

    /** Identifiant metier unique (UUID) expose au public / QR. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    /** Reference metier unique du support (ex. BUS-L12-4521). */
    @NotBlank
    @Size(max = 50)
    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    /** Libelle descriptif du support (ligne, vehicule, etc.). */
    @NotBlank
    @Size(max = 150)
    @Column(name = "label", nullable = false, length = 150)
    private String label;

    /** URL publique du QR Code (page de signalement). */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    /** Chemin du fichier image QR Code sur le serveur. */
    @Column(name = "qr_code_path", length = 500)
    private String qrCodePath;

    /** Date/heure de generation du QR Code. */
    @Column(name = "qr_date_creation")
    private Instant qrDateCreation;

    /** Date/heure d'impression physique du QR Code. */
    @Column(name = "qr_date_impression")
    private Instant qrDateImpression;

    /** Etat du QR Code (GENERATED, PRINTED, ACTIVE, DISABLED...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "qr_status", length = 30)
    private QrStatus qrStatus;

    /** Etat operationnel du support (ACTIVE, INACTIVE...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "support_status", nullable = false, length = 30)
    private SupportStatus supportStatus = SupportStatus.ACTIVE;

    /** Type de support (bus, metro, arret, etc.). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transport_support_type"))
    private SupportType supportType;

    /** Date/heure de creation de l'enregistrement. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Date/heure de derniere modification. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Version pour verrouillage optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (qrDateCreation == null) {
            qrDateCreation = now;
        }
        if (qrStatus == null) {
            qrStatus = QrStatus.GENERATED;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getTransportSupportId() {
        return transportSupportId;
    }

    public void setTransportSupportId(Long transportSupportId) {
        this.transportSupportId = transportSupportId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public Instant getQrDateCreation() {
        return qrDateCreation;
    }

    public void setQrDateCreation(Instant qrDateCreation) {
        this.qrDateCreation = qrDateCreation;
    }

    public Instant getQrDateImpression() {
        return qrDateImpression;
    }

    public void setQrDateImpression(Instant qrDateImpression) {
        this.qrDateImpression = qrDateImpression;
    }

    public QrStatus getQrStatus() {
        return qrStatus;
    }

    public void setQrStatus(QrStatus qrStatus) {
        this.qrStatus = qrStatus;
    }

    public SupportStatus getSupportStatus() {
        return supportStatus;
    }

    public void setSupportStatus(SupportStatus supportStatus) {
        this.supportStatus = supportStatus;
    }

    public SupportType getSupportType() {
        return supportType;
    }

    public void setSupportType(SupportType supportType) {
        this.supportType = supportType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
