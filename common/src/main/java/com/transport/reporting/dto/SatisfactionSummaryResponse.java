package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Indicateurs de perception voyageur.
 * Les pourcentages de niveau portent sur les avis reçus.
 * Le taux de participation porte sur les signalements clôturés éligibles au filtre.
 */
@Data
@Builder
public class SatisfactionSummaryResponse {

    private long eligibleClosedReports;
    private long evaluationsReceived;
    /** Part des dossiers clôturés éligibles qui ont un avis. */
    private double participationRate;
    private long satisfiedCount;
    private double satisfiedRate;
    private long partialCount;
    private double partialRate;
    private long dissatisfiedCount;
    private double dissatisfiedRate;
}
