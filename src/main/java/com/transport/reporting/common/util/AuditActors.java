package com.transport.reporting.common.util;

/**
 * Acteur courant pour le journal d'audit.
 * <p>
 * Tant qu'aucune authentification JWT n'est branchée, les opérations admin
 * sont attribuées à l'utilisateur seed {@code userId = 1}.
 * Remplacer cette logique par {@code SecurityContextHolder} le moment venu.
 */
public final class AuditActors {

    /** Identifiant de l'administrateur seed (DataInitializer). */
    public static final Long DEFAULT_ADMIN_USER_ID = 1L;

    private AuditActors() {
    }

    public static Long currentAdminUserId() {
        return DEFAULT_ADMIN_USER_ID;
    }
}
