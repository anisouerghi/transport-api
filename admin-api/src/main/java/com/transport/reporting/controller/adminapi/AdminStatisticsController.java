package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.StatisticsOverviewResponse;
import com.transport.reporting.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur admin — Rapports & Statistiques (squelette API).
 * Permission : {@code REPORT_STATISTICS_VIEW}.
 */
@RestController
@RequestMapping("/api/admin/statistics")
@Tag(name = "Admin - Rapports & Statistiques")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;
    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
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
}
