package com.transport.reporting.common.enums;

/**
 * Découpage de l'analyse. Chaque signalement appartient à un seul groupe feuille.
 */
public enum ReportAnalysisGrouping {
    NONE("Aucun regroupement"),
    REPORT_TYPE("Type de signalement"),
    SUPPORT_TYPE("Type de support"),
    SUPPORT("Support précis"),
    SUPPORT_TYPE_THEN_SUPPORT("Type de support puis support précis"),
    REPORT_TYPE_THEN_SUPPORT_TYPE("Type de signalement puis type de support"),
    SUPPORT_TYPE_THEN_REPORT_TYPE("Type de support puis type de signalement");

    private final String label;

    ReportAnalysisGrouping(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean hierarchical() {
        return this == SUPPORT_TYPE_THEN_SUPPORT
                || this == REPORT_TYPE_THEN_SUPPORT_TYPE
                || this == SUPPORT_TYPE_THEN_REPORT_TYPE;
    }
}
