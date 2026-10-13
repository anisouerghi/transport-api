package com.transport.reporting.service;

import com.transport.reporting.dto.ReportAnalysisDetail;
import com.transport.reporting.dto.ReportAnalysisResponse;
import com.transport.reporting.dto.ReportAnalysisRow;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportAnalysisExportServiceTest {

    private final ReportAnalysisExportService exporter = new ReportAnalysisExportService();

    @Test
    void pdfContainsTheOfficialHeader() {
        byte[] pdf = exporter.pdf(sample("Rapport mensuel des signalements – TRANSTU"));
        String header = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header);
        assertTrue(pdf.length > 1000);
    }

    @Test
    void excelIsAnXlsxWorkbook() {
        byte[] excel = exporter.excel(sample("Analyse des signalements par support"), List.of(
                ReportAnalysisDetail.builder()
                        .reference("SIG-1")
                        .reportTypeLabel("Réclamation")
                        .supportTypeLabel("Bus")
                        .supportLabel("Ligne 12")
                        .createdAt("01/10/2026 10:00")
                        .build()));
        assertEquals('P', excel[0]);
        assertEquals('K', excel[1]);
        assertTrue(excel.length > 1000);
    }

    private static ReportAnalysisResponse sample(String title) {
        return ReportAnalysisResponse.builder()
                .title(title)
                .subtitle("Octobre")
                .periodLabel("01/10/2026 – 13/10/2026")
                .reportTypeLabel("Tous")
                .supportTypeLabel("Bus")
                .supportLabel("Tous")
                .grouping("SUPPORT_TYPE")
                .groupingLabel("Type de support")
                .totalReports(19)
                .groupCount(2)
                .detailTruncated(false)
                .detailLimit(5000)
                .generatedAt("13/10/2026 09:00")
                .rows(List.of(
                        ReportAnalysisRow.builder().kind("GROUP").label("Bus").count(12L).rate(63.2).lastReportAt("01/10/2026 10:00").build(),
                        ReportAnalysisRow.builder().kind("GROUP").label("Métro").count(7L).rate(36.8).lastReportAt("02/10/2026 11:00").build(),
                        ReportAnalysisRow.builder().kind("TOTAL").label("Total").count(19L).rate(100.0).lastReportAt("02/10/2026 11:00").build()))
                .build();
    }
}
