package com.transport.reporting.dto;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.AuditResult;
import lombok.Data;

import java.time.Instant;

/**
 * Critères de recherche multicritère pour le journal d'audit.
 * Tous les champs sont optionnels et combinables.
 */
@Data
public class AuditLogCriteria {

    /** Filtre partiel sur username ou nom complet. */
    private String user;

    private Long userId;

    private AuditModule module;

    private AuditAction actionType;

    private Instant actionDateFrom;

    private Instant actionDateTo;

    private AuditResult result;

    /** Filtre partiel sur l'adresse IP. */
    private String ipAddress;

    private String entityName;

    private String entityId;
}
