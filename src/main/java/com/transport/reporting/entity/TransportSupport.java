package com.transport.reporting.entity;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "TRANSPORT_SUPPORT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transportSupportId")
    private Long transportSupportId;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "qrCodeUrl", length = 500)
    private String qrCodeUrl;

    @Column(name = "qrDateCreation")
    private Instant qrDateCreation;

    @Column(name = "qrDateImpression")
    private Instant qrDateImpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "qrStatus", length = 30)
    private QrStatus qrStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "supportStatus", nullable = false, length = 30)
    @Builder.Default
    private SupportStatus supportStatus = SupportStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supportTypeId", nullable = false)
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
