package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.DashboardResponse;
import com.transport.reporting.dto.ReportTypeCountResponse;
import com.transport.reporting.dto.ReportStatusCountResponse;
import com.transport.reporting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin - Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }


    @GetMapping
    @PreAuthorize("@perm.has('DASHBOARD', 'VIEW')")
    @Operation(summary = "Consulter le tableau de bord")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard()));
    }

    @GetMapping("/reports-by-type")
    @PreAuthorize("@perm.has('DASHBOARD', 'VIEW')")
    @Operation(
            summary = "Nombre de signalements par type",
            description = "Répartition des signalements par type, du plus fréquent au moins fréquent. "
                    + "Seuls les types ayant au moins un signalement sont retournés. "
                    + "Le libellé suit l'en-tête Accept-Language (fr/ar/en)."
    )
    public ResponseEntity<ApiResponse<List<ReportTypeCountResponse>>> reportsByType() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.countReportsByType()));
    }

    @GetMapping("/reports-by-status")
    @PreAuthorize("@perm.has('DASHBOARD', 'VIEW')")
    @Operation(
            summary = "Nombre de signalements par statut",
            description = "Répartition des signalements par statut, du plus fréquent au moins fréquent. "
                    + "Seuls les statuts ayant au moins un signalement sont retournés. "
                    + "Le libellé suit l'en-tête Accept-Language (fr/ar/en)."
    )
    public ResponseEntity<ApiResponse<List<ReportStatusCountResponse>>> reportsByStatus() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.countReportsByStatus()));
    }
}
