package com.transport.reporting.entity;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité Journal d'audit — table {@code audit_log}.
 * Trace les actions importantes réalisées dans l'administration (et opérations associées).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    /** Instant précis de l'action (date + heure). */
    @Column(name = "action_date", nullable = false)
    private Instant actionDate;

    /** Identifiant technique de l'utilisateur (nullable si action anonyme / système). */
    @Column(name = "user_id")
    private Long userId;

    /** Login de l'utilisateur au moment de l'action. */
    @Column(name = "username", length = 100)
    private String username;

    /** Nom complet affiché. */
    @Column(name = "user_full_name", length = 150)
    private String userFullName;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AuditAction actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 40)
    private AuditModule module;

    /** Nom logique de l'entité (ex. Report, AppUser). */
    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "browser", length = 120)
    private String browser;

    @Column(name = "operating_system", length = 120)
    private String operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    @Builder.Default
    private AuditResult result = AuditResult.SUCCESS;

    @PrePersist
    public void prePersist() {
        if (actionDate == null) {
            actionDate = Instant.now();
        }
        if (result == null) {
            result = AuditResult.SUCCESS;
        }
    }
}
