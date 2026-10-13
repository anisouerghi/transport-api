package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.ReportAnalysisOption;
import com.transport.reporting.dto.ReportAnalysisOptionsResponse;
import com.transport.reporting.dto.ReportAnalysisRequest;
import com.transport.reporting.dto.ReportAnalysisResponse;
import com.transport.reporting.dto.SatisfactionCommentResponse;
import com.transport.reporting.dto.SatisfactionSummaryResponse;
import com.transport.reporting.dto.StatisticsOverviewResponse;
import com.transport.reporting.service.ReportAnalysisExportService;
import com.transport.reporting.service.ReportAnalysisService;
import com.transport.reporting.service.ReportSatisfactionService;
import com.transport.reporting.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur admin — Rapports & Statistiques (squelette API).
 * Permission : {@code REPORT_STATISTICS_VIEW}.
 */
@RestController
@RequestMapping("/api/admin/statistics")
@Tag(name = "Admin - Rapports & Statistiques")
public class AdminStatisticsController {

    private static final DateTimeFormatter FILE_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final StatisticsService statisticsService;
    private final ReportSatisfactionService satisfactionService;
    private final ReportAnalysisService reportAnalysisService;
    private final ReportAnalysisExportService reportAnalysisExportService;

    public AdminStatisticsController(
            StatisticsService statisticsService,
            ReportSatisfactionService satisfactionService,
            ReportAnalysisService reportAnalysisService,
            ReportAnalysisExportService reportAnalysisExportService) {
        this.statisticsService = statisticsService;
        this.satisfactionService = satisfactionService;
        this.reportAnalysisService = reportAnalysisService;
        this.reportAnalysisExportService = reportAnalysisExportService;
    }


    @GetMapping("/overview")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(
            summary = "Vue d'ensemble des statistiques",
            description = "Indicateurs de base (volumes). Point d'entrée extensible pour les futurs tableaux de bord."
    )
    public ResponseEntity<ApiResponse<StatisticsOverviewResponse>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getOverview()));
    }

    @GetMapping("/satisfaction")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(
            summary = "Indicateurs de satisfaction voyageur",
            description = "Perception de la réponse TRANSTU, pas une note d'agent. "
                    + "Les pourcentages de niveau portent sur les avis reçus. "
                    + "Le taux de participation a pour dénominateur les signalements clôturés du filtre. "
                    + "Période en dates UTC de clôture. Filtres optionnels : nature (reportTypeId) et support."
    )
    public ResponseEntity<ApiResponse<SatisfactionSummaryResponse>> satisfaction(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long reportTypeId,
            @RequestParam(required = false) Long transportSupportId) {
        return ResponseEntity.ok(ApiResponse.ok(
                satisfactionService.summarize(from, to, reportTypeId, transportSupportId)));
    }

    @GetMapping("/satisfaction/comments")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(
            summary = "Commentaires d'évaluation",
            description = "Commentaires facultatifs, sans identité du voyageur. Même permission que les indicateurs."
    )
    public ResponseEntity<ApiResponse<PageResponse<SatisfactionCommentResponse>>> satisfactionComments(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long reportTypeId,
            @RequestParam(required = false) Long transportSupportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                satisfactionService.comments(from, to, reportTypeId, transportSupportId, page, size)));
    }

    @GetMapping("/analysis/options")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(summary = "Listes des filtres de l'analyse des signalements")
    public ResponseEntity<ApiResponse<ReportAnalysisOptionsResponse>> analysisOptions() {
        return ResponseEntity.ok(ApiResponse.ok(reportAnalysisService.options()));
    }

    @GetMapping("/analysis/supports")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(summary = "Supports filtrables par type, référence ou libellé")
    public ResponseEntity<ApiResponse<List<ReportAnalysisOption>>> analysisSupports(
            @RequestParam(required = false) Long supportTypeId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(reportAnalysisService.supports(supportTypeId, q)));
    }

    @PostMapping("/analysis")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(summary = "Aperçu agrégé des signalements")
    public ResponseEntity<ApiResponse<ReportAnalysisResponse>> analysis(
            @Valid @RequestBody ReportAnalysisRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(reportAnalysisService.analyze(request)));
    }

    @PostMapping("/analysis/pdf")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(summary = "Rapport PDF JasperReports de l'analyse")
    public ResponseEntity<byte[]> analysisPdf(@Valid @RequestBody ReportAnalysisRequest request) {
        ReportAnalysisResponse analysis = reportAnalysisService.analyze(request);
        return file(reportAnalysisExportService.pdf(analysis), "application/pdf", fileName(request, "pdf"));
    }

    @PostMapping("/analysis/excel")
    @PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
    @Operation(summary = "Classeur Excel JasperReports de l'analyse")
    public ResponseEntity<byte[]> analysisExcel(@Valid @RequestBody ReportAnalysisRequest request) {
        ReportAnalysisResponse analysis = reportAnalysisService.analyze(request);
        byte[] body = reportAnalysisExportService.excel(analysis, reportAnalysisService.details(request));
        return file(
                body,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileName(request, "xlsx"));
    }

    private static ResponseEntity<byte[]> file(byte[] body, String mediaType, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename)
                        .build()
                        .toString())
                .body(body);
    }

    private static String fileName(ReportAnalysisRequest request, String extension) {
        LocalDate month = request.getFrom() != null ? request.getFrom() : LocalDate.now(ZoneOffset.UTC);
        return "rapport-analyse-signalements-" + FILE_MONTH.format(month) + "." + extension;
    }
}
