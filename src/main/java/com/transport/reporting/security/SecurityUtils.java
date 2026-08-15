package com.transport.reporting.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Accès à l'utilisateur authentifié courant.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return Optional.empty();
        }
        return Optional.of((UserPrincipal) authentication.getPrincipal());
    }

    public static Long currentUserIdOrNull() {
        return currentUser().map(UserPrincipal::getUserId).orElse(null);
    }

    public static Long requireCurrentUserId() {
        return currentUser()
                .map(UserPrincipal::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
