package com.transport.reporting.security;

import java.util.Collections;
import java.util.Set;

/**
 * Périmètre des types de signalement autorisés pour l'utilisateur courant.
 * <ul>
 *   <li>{@link #unrestricted()} — rôle ADMIN : aucune restriction de type ;</li>
 *   <li>{@link #of(Set)} — union des types des rôles actifs ;</li>
 *   <li>ensemble vide — aucun signalement consultable (jamais « tous les types »).</li>
 * </ul>
 */
public final class ReportTypeScope {

    private final boolean unrestricted;
    private final Set<Long> reportTypeIds;

    private ReportTypeScope(boolean unrestricted, Set<Long> reportTypeIds) {
        this.unrestricted = unrestricted;
        this.reportTypeIds = reportTypeIds == null ? Set.of() : Set.copyOf(reportTypeIds);
    }

    public static ReportTypeScope unrestricted() {
        return new ReportTypeScope(true, Set.of());
    }

    public static ReportTypeScope of(Set<Long> reportTypeIds) {
        return new ReportTypeScope(false, reportTypeIds == null ? Set.of() : reportTypeIds);
    }

    public static ReportTypeScope none() {
        return new ReportTypeScope(false, Set.of());
    }

    public boolean isUnrestricted() {
        return unrestricted;
    }

    public Set<Long> reportTypeIds() {
        return unrestricted ? Collections.emptySet() : reportTypeIds;
    }

    public boolean allows(Long reportTypeId) {
        if (unrestricted) {
            return true;
        }
        return reportTypeId != null && reportTypeIds.contains(reportTypeId);
    }

    public boolean isEmpty() {
        return !unrestricted && reportTypeIds.isEmpty();
    }
}
