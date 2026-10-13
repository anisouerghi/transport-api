package com.transport.reporting.service;

import com.transport.reporting.common.enums.ReportAnalysisGrouping;
import com.transport.reporting.dto.ReportAnalysisRow;
import com.transport.reporting.repository.ReportAnalysisQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportAnalysisAggregatorTest {

    @Test
    void hierarchicalTotalsDoNotDoubleCountReports() {
        List<ReportAnalysisQuery.RawBucket> buckets = List.of(
                new ReportAnalysisQuery.RawBucket("Bus", "Support A", 12, Instant.parse("2026-10-01T10:00:00Z")),
                new ReportAnalysisQuery.RawBucket("Bus", "Support B", 7, Instant.parse("2026-10-02T10:00:00Z")),
                new ReportAnalysisQuery.RawBucket("Métro", "Support C", 18, Instant.parse("2026-10-03T10:00:00Z")));

        List<ReportAnalysisRow> rows = ReportAnalysisAggregator.rows(
                ReportAnalysisGrouping.SUPPORT_TYPE_THEN_SUPPORT, buckets, 37, Instant.parse("2026-10-03T10:00:00Z"));

        long leaves = rows.stream().filter(row -> "GROUP".equals(row.getKind())).mapToLong(ReportAnalysisRow::getCount).sum();
        long subtotals = rows.stream().filter(row -> "SUBTOTAL".equals(row.getKind())).mapToLong(ReportAnalysisRow::getCount).sum();
        ReportAnalysisRow total = rows.get(rows.size() - 1);

        assertEquals(37, leaves);
        assertEquals(37, subtotals);
        assertEquals(3, ReportAnalysisAggregator.groupCount(rows));
        assertEquals("TOTAL", total.getKind());
        assertEquals(37L, total.getCount());
        assertEquals(100.0, total.getRate());
        assertEquals(19L, rows.get(0).getCount());
    }

    @Test
    void flatGroupsUseTheDistinctTotal() {
        List<ReportAnalysisQuery.RawBucket> buckets = List.of(
                new ReportAnalysisQuery.RawBucket("", "Bus", 125, null),
                new ReportAnalysisQuery.RawBucket("", "Métro", 84, null));

        List<ReportAnalysisRow> rows = ReportAnalysisAggregator.rows(
                ReportAnalysisGrouping.SUPPORT_TYPE, buckets, 209, null);

        assertEquals(209L, rows.get(rows.size() - 1).getCount());
        assertEquals(2, ReportAnalysisAggregator.groupCount(rows));
    }

    @Test
    void emptyPeriodHasNoRows() {
        assertTrue(ReportAnalysisAggregator.rows(ReportAnalysisGrouping.NONE, List.of(), 0, null).isEmpty());
    }

    @Test
    void groupedSqlCountsDistinctReports() {
        String sql = ReportAnalysisQuery.select(ReportAnalysisGrouping.SUPPORT);
        assertTrue(sql.contains("COUNT(DISTINCT r.report_id)"));
        assertTrue(!sql.contains("COUNT(*)"));
    }
}
