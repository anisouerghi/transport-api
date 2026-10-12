package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.SatisfactionCommentResponse;
import com.transport.reporting.dto.SatisfactionSummaryResponse;
import com.transport.reporting.dto.StatisticsOverviewResponse;
import com.transport.reporting.service.ReportSatisfactionService;
import com.transport.reporting.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Contrôleur admin — Rapports & Statistiques (squelette API).
 * Permission : {@code REPORT_STATISTICS_VIEW}.
 */
@RestController
@RequestMapping("/api/admin/statistics")
@Tag(name = "Admin - Rapports & Statistiques")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;
    private final ReportSatisfactionService satisfactionService;

    public AdminStatisticsController(
            StatisticsService statisticsService,
            ReportSatisfactionService satisfactionService) {
        this.statisticsService = statisticsService;
        this.satisfactionService = satisfactionService;
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
}
