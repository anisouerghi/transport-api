package com.transport.reporting.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Vérificateur centralisé des permissions dynamiques.
 * <p>
 * Usage dans les contrôleurs :
 * {@code @PreAuthorize("@perm.has('REPORT', 'VIEW')")}
 * <p>
 * La convention de code est {@code MODULE_ACTION} (ex. {@code REPORT_VIEW}),
 * alignée sur les permissions stockées en base. Aucune liste de permissions
 * n'est figée ici : seules les authorities du JWT / SecurityContext sont testées.
 */
@Component("perm")
public class PermissionChecker {

    /**
     * Vérifie que l'utilisateur authentifié possède la permission module × action.
     */
    public boolean has(String module, String action) {
        if (module == null || action == null) {
            return false;
        }
        String code = module.trim().toUpperCase() + "_" + action.trim().toUpperCase();
        return hasCode(code);
    }

    /** Vérifie une permission par son code exact (ex. REPORT_REPLY). */
    public boolean hasCode(String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (permissionCode.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** Au moins une des actions du module. */
    public boolean hasAny(String module, String... actions) {
        if (actions == null) {
            return false;
        }
        for (String action : actions) {
            if (has(module, action)) {
                return true;
            }
        }
        return false;
    }
}
