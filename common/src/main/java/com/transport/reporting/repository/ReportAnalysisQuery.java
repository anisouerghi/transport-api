package com.transport.reporting.repository;

import com.transport.reporting.common.enums.ReportAnalysisGrouping;
import com.transport.reporting.dto.ReportAnalysisDetail;
import com.transport.reporting.dto.ReportAnalysisOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrégations MySQL de l'analyse. Les jointures sont des liens plusieurs-vers-un :
 * {@code COUNT(DISTINCT report_id)} évite qu'une jointure future ne gonfle les totaux.
 */
@Repository
public class ReportAnalysisQuery {

    static final DateTimeFormatter DETAIL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Tunis"));

    private static final String JOINS = """
            FROM report r
            LEFT JOIN report_type rt ON rt.report_type_id = r.report_type_id
            LEFT JOIN transport_support ts ON ts.transport_support_id = r.transport_support_id
            LEFT JOIN support_type st ON st.support_type_id = ts.support_type_id
            """;

    private final JdbcTemplate jdbc;

    public ReportAnalysisQuery(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public Total total(Filter filter) {
        String sql = "SELECT COUNT(DISTINCT r.report_id), MAX(r.creation_date) " + JOINS + filter.where();
        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return new Total(0, null);
            }
            Timestamp last = rs.getTimestamp(2);
            return new Total(rs.getLong(1), last == null ? null : last.toInstant());
        }, filter.args().toArray());
    }

    public List<RawBucket> buckets(Filter filter, ReportAnalysisGrouping grouping) {
        if (grouping == null || grouping == ReportAnalysisGrouping.NONE) {
            return List.of();
        }
        String sql = "SELECT " + select(grouping) + " "
                + JOINS + filter.where()
                + " GROUP BY " + groupBy(grouping)
                + " HAVING COUNT(DISTINCT r.report_id) > 0";
        return jdbc.query(sql, (rs, rowNum) -> {
            Timestamp last = rs.getTimestamp("last_at");
            return new RawBucket(
                    rs.getString("parent_label"),
                    rs.getString("child_label"),
                    rs.getLong("report_count"),
                    last == null ? null : last.toInstant());
        }, filter.args().toArray());
    }

    public List<ReportAnalysisDetail> details(Filter filter, int limit) {
        String sql = """
                SELECT r.reference,
                       COALESCE(NULLIF(TRIM(rt.label_fr), ''), NULLIF(TRIM(rt.label), ''), 'Type non défini'),
                       COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type de support'),
                       COALESCE(NULLIF(TRIM(ts.label), ''), NULLIF(TRIM(ts.reference), ''), 'Sans support'),
                       r.creation_date
                """ + JOINS + filter.where() + " ORDER BY r.creation_date DESC LIMIT ?";
        List<Object> args = new ArrayList<>(filter.args());
        args.add(limit);
        return jdbc.query(sql, (rs, rowNum) -> {
            Timestamp created = rs.getTimestamp(5);
            return ReportAnalysisDetail.builder()
                    .reference(rs.getString(1))
                    .reportTypeLabel(rs.getString(2))
                    .supportTypeLabel(rs.getString(3))
                    .supportLabel(rs.getString(4))
                    .createdAt(created == null ? "" : DETAIL_DATE.format(created.toInstant()))
                    .build();
        }, args.toArray());
    }

    public List<ReportAnalysisOption> reportTypes() {
        return jdbc.query("""
                SELECT report_type_id, COALESCE(NULLIF(TRIM(label_fr), ''), label)
                FROM report_type
                WHERE active = 1
                ORDER BY priority, label_fr, label
                """, (rs, rowNum) -> option(rs.getLong(1), rs.getString(2)));
    }

    public List<ReportAnalysisOption> supportTypes() {
        return jdbc.query("""
                SELECT support_type_id, COALESCE(NULLIF(TRIM(label_fr), ''), label)
                FROM support_type
                ORDER BY label
                """, (rs, rowNum) -> option(rs.getLong(1), rs.getString(2)));
    }

    public List<ReportAnalysisOption> supports(Long supportTypeId, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT ts.transport_support_id,
                       CONCAT(COALESCE(NULLIF(TRIM(ts.reference), ''), 'Sans référence'),
                              ' – ',
                              COALESCE(NULLIF(TRIM(ts.label), ''), 'Sans libellé'))
                FROM transport_support ts
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (supportTypeId != null) {
            sql.append(" AND ts.support_type_id = ?");
            args.add(supportTypeId);
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (ts.reference LIKE ? OR ts.label LIKE ?)");
            String like = "%" + query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY ts.label, ts.reference LIMIT 50");
        return jdbc.query(sql.toString(), (rs, rowNum) -> option(rs.getLong(1), rs.getString(2)), args.toArray());
    }

    public String reportTypeLabel(Long id) {
        return one("SELECT COALESCE(NULLIF(TRIM(label_fr), ''), label) FROM report_type WHERE report_type_id = ?", id);
    }

    public String supportTypeLabel(Long id) {
        return one("SELECT COALESCE(NULLIF(TRIM(label_fr), ''), label) FROM support_type WHERE support_type_id = ?", id);
    }

    public String supportLabel(Long id) {
        return one("""
                SELECT CONCAT(COALESCE(NULLIF(TRIM(reference), ''), 'Sans référence'),
                              ' – ',
                              COALESCE(NULLIF(TRIM(label), ''), 'Sans libellé'))
                FROM transport_support WHERE transport_support_id = ?
                """, id);
    }

    public static String select(ReportAnalysisGrouping grouping) {
        return switch (grouping) {
            case REPORT_TYPE -> """
                    '' AS parent_label,
                    COALESCE(NULLIF(TRIM(rt.label_fr), ''), NULLIF(TRIM(rt.label), ''), 'Type non défini') AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case SUPPORT_TYPE -> """
                    '' AS parent_label,
                    COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type de support') AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case SUPPORT -> """
                    '' AS parent_label,
                    CONCAT(
                        COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type'),
                        ' – ',
                        COALESCE(NULLIF(TRIM(ts.label), ''), NULLIF(TRIM(ts.reference), ''), 'Sans support')
                    ) AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case SUPPORT_TYPE_THEN_SUPPORT -> """
                    COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type de support') AS parent_label,
                    COALESCE(NULLIF(TRIM(ts.label), ''), NULLIF(TRIM(ts.reference), ''), 'Sans support') AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case REPORT_TYPE_THEN_SUPPORT_TYPE -> """
                    COALESCE(NULLIF(TRIM(rt.label_fr), ''), NULLIF(TRIM(rt.label), ''), 'Type non défini') AS parent_label,
                    COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type de support') AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case SUPPORT_TYPE_THEN_REPORT_TYPE -> """
                    COALESCE(NULLIF(TRIM(st.label_fr), ''), NULLIF(TRIM(st.label), ''), 'Sans type de support') AS parent_label,
                    COALESCE(NULLIF(TRIM(rt.label_fr), ''), NULLIF(TRIM(rt.label), ''), 'Type non défini') AS child_label,
                    COUNT(DISTINCT r.report_id) AS report_count,
                    MAX(r.creation_date) AS last_at
                    """;
            case NONE -> throw new IllegalArgumentException("Aucun regroupement SQL");
        };
    }

    static String groupBy(ReportAnalysisGrouping grouping) {
        return switch (grouping) {
            case REPORT_TYPE -> "r.report_type_id, child_label";
            case SUPPORT_TYPE -> "st.support_type_id, child_label";
            case SUPPORT -> "ts.transport_support_id, child_label";
            case SUPPORT_TYPE_THEN_SUPPORT -> "st.support_type_id, parent_label, ts.transport_support_id, child_label";
            case REPORT_TYPE_THEN_SUPPORT_TYPE -> "r.report_type_id, parent_label, st.support_type_id, child_label";
            case SUPPORT_TYPE_THEN_REPORT_TYPE -> "st.support_type_id, parent_label, r.report_type_id, child_label";
            case NONE -> throw new IllegalArgumentException("Aucun regroupement SQL");
        };
    }

    private String one(String sql, Long id) {
        List<String> labels = jdbc.query(sql, (rs, rowNum) -> rs.getString(1), id);
        return labels.isEmpty() ? null : labels.get(0);
    }

    private static ReportAnalysisOption option(long id, String label) {
        return ReportAnalysisOption.builder().id(id).label(label).build();
    }

    public record Filter(String where, List<Object> args) {
    }

    public record Total(long count, Instant lastAt) {
    }

    public record RawBucket(String parentLabel, String childLabel, long count, Instant lastAt) {
    }
}
