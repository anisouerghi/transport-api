package com.transport.reporting.dto;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import com.transport.reporting.entity.AppUser;
import lombok.Builder;
import lombok.Data;

/**
 * Événement d'audit à enregistrer depuis les services métier.
 * Les métadonnées HTTP (IP, User-Agent) sont enrichies automatiquement si absentes.
 */
@Data
@Builder
public class AuditLogEvent {

    private Long userId;
    private String username;
    private String userFullName;
    private AuditAction actionType;
    private AuditModule module;
    private String entityName;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String description;
    @Builder.Default
    private AuditResult result = AuditResult.SUCCESS;
    private String ipAddress;
    private String userAgent;

    /**
     * Renseigne l'acteur à partir d'un {@link AppUser} (si non null).
     */
    public AuditLogEvent withUser(AppUser user) {
        if (user != null) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.userFullName = user.getName();
        }
        return this;
    }

    public static AuditLogEvent of(AuditAction action, AuditModule module, String description) {
        return AuditLogEvent.builder()
                .actionType(action)
                .module(module)
                .description(description)
                .result(AuditResult.SUCCESS)
                .build();
    }
}
