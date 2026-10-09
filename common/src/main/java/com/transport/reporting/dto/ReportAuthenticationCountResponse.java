package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Répartition des signalements anonymes vs authentifiés (KPI tableau de bord).
 *
 * <p>Un signalement est considéré <strong>authentifié</strong> lorsque son
 * voyageur a validé son adresse e-mail ({@code passenger.email_verified = 1}).
 * Tous les autres cas comptent comme <strong>anonymes</strong>.</p>
 */
@Data
@Builder
public class ReportAuthenticationCountResponse {

    private long total;
    private long authenticated;
    private long anonymous;
}
