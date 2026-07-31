package com.transport.reporting.common.enums;

/**
 * Types d'actions tracées dans le journal d'audit.
 * Extensible : ajouter de nouvelles valeurs sans migration de schéma (VARCHAR).
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    CONSULTATION,
    EXPORT,
    SEARCH,
    REPLY,
    STATUS_CHANGE,
    UPLOAD,
    OTHER
}
