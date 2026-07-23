package com.transport.reporting.modules.support.entity;

import com.transport.reporting.common.enums.TypeSupport;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Support physique portant un QR Code (bus, métro, train, station...).
 */
@Entity
@Table(name = "support_transport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Support {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeSupport type;

    @Column(length = 500)
    private String qrCodeUrl;

    @Column(nullable = false)
    private Instant qrDateCreation;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (qrDateCreation == null) {
            qrDateCreation = Instant.now();
        }
    }
}
