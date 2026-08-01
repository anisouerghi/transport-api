package com.transport.reporting.common.util;

import com.transport.reporting.security.SecurityUtils;

/**
 * Acteur courant pour le journal d'audit.
 * Utilise le JWT / SecurityContext lorsque disponible.
 */
public final class AuditActors {

    /** Identifiant de l'administrateur seed (fallback hors contexte HTTP). */
    public static final Long DEFAULT_ADMIN_USER_ID = 1L;

    private AuditActors() {
    }

    public static Long currentAdminUserId() {
        Long id = SecurityUtils.currentUserIdOrNull();
        return id != null ? id : DEFAULT_ADMIN_USER_ID;
    }
}
