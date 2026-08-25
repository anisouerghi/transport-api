package com.transport.reporting.dto;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO réponse d'une entrée du journal d'audit.
 */
@Data
@Builder
public class AuditLogResponse {

    private Long auditLogId;
    private Instant actionDate;
    private Long userId;
    private String username;
    private String userFullName;
    private String ipAddress;
    private AuditAction actionType;
    private AuditModule module;
    private String entityName;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String description;
    private String userAgent;
    private String browser;
    private String operatingSystem;
    private AuditResult result;
}
