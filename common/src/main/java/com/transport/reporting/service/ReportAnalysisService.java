package com.transport.reporting.service;

import com.transport.reporting.common.enums.ReportAnalysisGrouping;
import com.transport.reporting.dto.ReportAnalysisDetail;
import com.transport.reporting.dto.ReportAnalysisOption;
import com.transport.reporting.dto.ReportAnalysisOptionsResponse;
import com.transport.reporting.dto.ReportAnalysisRequest;
import com.transport.reporting.dto.ReportAnalysisResponse;
import com.transport.reporting.dto.ReportAnalysisRow;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.ReportAnalysisQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportAnalysisService {

    public static final String DEFAULT_TITLE = "Rapport d'analyse des signalements – TRANSTU";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Tunis"));
    private static final int EXCEL_HARD_LIMIT = 1_048_576;

    private final ReportAnalysisQuery query;
    private final int detailLimit;
    private final int excelRowLimit;

    public ReportAnalysisService(
            ReportAnalysisQuery query,
            @Value("${reporting.analysis.detail-limit:5000}") int detailLimit,
            @Value("${reporting.analysis.excel-row-limit:1048576}") int excelRowLimit) {
        this.query = query;
        this.detailLimit = Math.max(1, detailLimit);
        this.excelRowLimit = Math.min(EXCEL_HARD_LIMIT, Math.max(1, excelRowLimit));
    }

    public ReportAnalysisOptionsResponse options() {
        return ReportAnalysisOptionsResponse.builder()
                .reportTypes(query.reportTypes())
                .supportTypes(query.supportTypes())
                .build();
    }

    public List<ReportAnalysisOption> supports(Long supportTypeId, String search) {
        return query.supports(supportTypeId, search);
    }

    public ReportAnalysisResponse analyze(ReportAnalysisRequest request) {
        Prepared prepared = prepare(request);
        ReportAnalysisQuery.Total total = query.total(prepared.filter());
        List<ReportAnalysisQuery.RawBucket> buckets = query.buckets(prepared.filter(), prepared.grouping());
        List<ReportAnalysisRow> rows = ReportAnalysisAggregator.rows(
                prepared.grouping(), buckets, total.count(), total.lastAt());
        boolean truncated = prepared.grouping() == ReportAnalysisGrouping.NONE && total.count() > detailLimit;
        return response(prepared, total.count(), rows, truncated);
    }

    /** Détail autorisé seulement sans regroupement, plafonné, sans donnée personnelle. */
    public List<ReportAnalysisDetail> details(ReportAnalysisRequest request) {
        Prepared prepared = prepare(request);
        if (prepared.grouping() != ReportAnalysisGrouping.NONE) {
            return List.of();
        }
        ReportAnalysisQuery.Total total = query.total(prepared.filter());
        if (total.count() > excelRowLimit) {
            throw new BusinessException(
                    "Le volume dépasse la limite d'export (" + excelRowLimit + " lignes). Affinez la période ou les filtres.",
                    "EXPORT_TOO_LARGE");
        }
        return query.details(prepared.filter(), Math.min(detailLimit, excelRowLimit));
    }

    public int detailLimit() {
        return detailLimit;
    }

    private ReportAnalysisResponse response(
            Prepared prepared, long total, List<ReportAnalysisRow> rows, boolean truncated) {
        return ReportAnalysisResponse.builder()
                .title(prepared.title())
                .subtitle(prepared.subtitle())
                .periodLabel(prepared.periodLabel())
                .reportTypeLabel(prepared.reportTypeLabel())
                .supportTypeLabel(prepared.supportTypeLabel())
                .supportLabel(prepared.supportLabel())
                .grouping(prepared.grouping().name())
                .groupingLabel(prepared.grouping().getLabel())
                .totalReports(total)
                .groupCount(ReportAnalysisAggregator.groupCount(rows))
                .detailTruncated(truncated)
                .detailLimit(detailLimit)
                .generatedAt(GENERATED.format(Instant.now()))
                .rows(rows)
                .build();
    }

    private Prepared prepare(ReportAnalysisRequest request) {
        ReportAnalysisRequest safe = request == null ? new ReportAnalysisRequest() : request;
        if (safe.getFrom() != null && safe.getTo() != null && safe.getFrom().isAfter(safe.getTo())) {
            throw new BusinessException(
                    "La date de début doit être antérieure ou égale à la date de fin.",
                    "INVALID_PERIOD");
        }
        ReportAnalysisGrouping grouping = safe.getGrouping() == null
                ? ReportAnalysisGrouping.NONE
                : safe.getGrouping();
        String title = safe.getTitle() == null || safe.getTitle().isBlank()
                ? DEFAULT_TITLE
                : safe.getTitle().trim();
        String subtitle = safe.getSubtitle() == null ? "" : safe.getSubtitle().trim();
        String reportTypeLabel = safe.getReportTypeId() == null
                ? "Tous"
                : required(query.reportTypeLabel(safe.getReportTypeId()), "Type de signalement introuvable.");
        String supportTypeLabel = safe.getSupportTypeId() == null
                ? "Tous"
                : required(query.supportTypeLabel(safe.getSupportTypeId()), "Type de support introuvable.");
        String supportLabel = safe.getTransportSupportId() == null
                ? "Tous"
                : required(query.supportLabel(safe.getTransportSupportId()), "Support introuvable.");
        return new Prepared(
                title,
                subtitle,
                period(safe.getFrom(), safe.getTo()),
                reportTypeLabel,
                supportTypeLabel,
                supportLabel,
                grouping,
                filter(safe));
    }

    private static String required(String label, String missing) {
        if (label == null || label.isBlank()) {
            throw new BusinessException(missing, "ANALYSIS_FILTER_INVALID");
        }
        return label;
    }

    private static String period(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "Toutes les dates";
        }
        if (from != null && to != null) {
            return DAY.format(from) + " – " + DAY.format(to);
        }
        if (from != null) {
            return "À partir du " + DAY.format(from);
        }
        return "Jusqu'au " + DAY.format(to);
    }

    private static ReportAnalysisQuery.Filter filter(ReportAnalysisRequest request) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (request.getFrom() != null) {
            where.append(" AND r.creation_date >= ?");
            args.add(Timestamp.from(request.getFrom().atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (request.getTo() != null) {
            where.append(" AND r.creation_date < ?");
            args.add(Timestamp.from(request.getTo().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (request.getReportTypeId() != null) {
            where.append(" AND r.report_type_id = ?");
            args.add(request.getReportTypeId());
        }
        if (request.getSupportTypeId() != null) {
            where.append(" AND st.support_type_id = ?");
            args.add(request.getSupportTypeId());
        }
        if (request.getTransportSupportId() != null) {
            where.append(" AND r.transport_support_id = ?");
            args.add(request.getTransportSupportId());
        }
        return new ReportAnalysisQuery.Filter(where.toString(), args);
    }

    private record Prepared(
            String title,
            String subtitle,
            String periodLabel,
            String reportTypeLabel,
            String supportTypeLabel,
            String supportLabel,
            ReportAnalysisGrouping grouping,
            ReportAnalysisQuery.Filter filter) {
    }
}
