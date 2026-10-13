package com.transport.reporting.service;

import com.transport.reporting.common.enums.ReportAnalysisGrouping;
import com.transport.reporting.dto.ReportAnalysisRow;
import com.transport.reporting.repository.ReportAnalysisQuery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Transforme les seaux SQL en lignes d'aperçu, sans second passage en base. */
public final class ReportAnalysisAggregator {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Tunis"));

    private ReportAnalysisAggregator() {
    }

    public static List<ReportAnalysisRow> rows(
            ReportAnalysisGrouping grouping,
            List<ReportAnalysisQuery.RawBucket> buckets,
            long total,
            Instant overallLast) {
        if (total <= 0) {
            return List.of();
        }
        if (grouping == null || grouping == ReportAnalysisGrouping.NONE) {
            return List.of(row("GROUP", "Tous les signalements", total, total, overallLast));
        }
        List<ReportAnalysisQuery.RawBucket> leaves = buckets.stream()
                .filter(bucket -> bucket.count() > 0)
                .sorted(Comparator
                        .comparing((ReportAnalysisQuery.RawBucket bucket) -> text(bucket.parentLabel()))
                        .thenComparing(bucket -> text(bucket.childLabel())))
                .toList();
        if (grouping.hierarchical()) {
            return hierarchical(leaves, total, overallLast);
        }
        List<ReportAnalysisRow> rows = new ArrayList<>();
        for (ReportAnalysisQuery.RawBucket bucket : leaves) {
            rows.add(row("GROUP", text(bucket.childLabel()), bucket.count(), total, bucket.lastAt()));
        }
        rows.add(row("TOTAL", "Total", total, total, overallLast));
        return rows;
    }

    public static int groupCount(List<ReportAnalysisRow> rows) {
        return (int) rows.stream().filter(row -> "GROUP".equals(row.getKind())).count();
    }

    public static double rate(long part, long total) {
        if (total <= 0 || part <= 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    private static List<ReportAnalysisRow> hierarchical(
            List<ReportAnalysisQuery.RawBucket> leaves, long total, Instant overallLast) {
        Map<String, List<ReportAnalysisQuery.RawBucket>> byParent = new LinkedHashMap<>();
        for (ReportAnalysisQuery.RawBucket bucket : leaves) {
            byParent.computeIfAbsent(text(bucket.parentLabel()), key -> new ArrayList<>()).add(bucket);
        }
        List<ReportAnalysisRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<ReportAnalysisQuery.RawBucket>> entry : byParent.entrySet()) {
            long subtotal = entry.getValue().stream().mapToLong(ReportAnalysisQuery.RawBucket::count).sum();
            Instant parentLast = null;
            for (ReportAnalysisQuery.RawBucket bucket : entry.getValue()) {
                parentLast = latest(parentLast, bucket.lastAt());
            }
            rows.add(row("SUBTOTAL", entry.getKey(), subtotal, total, parentLast));
            for (ReportAnalysisQuery.RawBucket bucket : entry.getValue()) {
                rows.add(row("GROUP", text(bucket.childLabel()), bucket.count(), total, bucket.lastAt()));
            }
        }
        rows.add(row("TOTAL", "Total", total, total, overallLast));
        return rows;
    }

    private static ReportAnalysisRow row(String kind, String label, long count, long total, Instant lastAt) {
        return ReportAnalysisRow.builder()
                .kind(kind)
                .label(label)
                .count(count)
                .rate(rate(count, total))
                .lastReportAt(lastAt == null ? "" : DATE.format(lastAt))
                .build();
    }

    private static Instant latest(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "Non défini" : value;
    }
}
