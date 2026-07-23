package com.transport.reporting.entity;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transport_support")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transport_support_id")
    private Long transportSupportId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "qr_date_creation")
    private Instant qrDateCreation;

    @Column(name = "qr_date_impression")
    private Instant qrDateImpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_status", length = 30)
    private QrStatus qrStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_status", nullable = false, length = 30)
    @Builder.Default
    private SupportStatus supportStatus = SupportStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_type_id", nullable = false)
    private SupportType supportType;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (qrDateCreation == null) {
            qrDateCreation = Instant.now();
        }
        if (qrStatus == null) {
            qrStatus = QrStatus.GENERATED;
        }
    }
}
