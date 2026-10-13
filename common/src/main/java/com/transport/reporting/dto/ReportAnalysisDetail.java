package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;

/** Ligne de détail sans identité du voyageur ni commentaire. */
@Getter
@Builder
public class ReportAnalysisDetail {
    private final String reference;
    private final String reportTypeLabel;
    private final String supportTypeLabel;
    private final String supportLabel;
    private final String createdAt;
}
