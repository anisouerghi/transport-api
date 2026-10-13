package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;

/** Ligne d'aperçu et de rapport. GROUP est un groupe feuille, SUBTOTAL un total de niveau, TOTAL le total général. */
@Getter
@Builder
public class ReportAnalysisRow {
    private final String kind;
    private final String label;
    private final Long count;
    private final Double rate;
    private final String lastReportAt;
}
