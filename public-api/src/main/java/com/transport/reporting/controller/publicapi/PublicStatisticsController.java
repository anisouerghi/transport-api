package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.SupportTypeCountResponse;
import com.transport.reporting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur public exposant des statistiques agrégées (aucun droit requis).
 *
 * <p>Les données exposées sont uniquement des compteurs agrégés, sans
 * information nominative.</p>
 */
@RestController
@RequestMapping("/api/public/statistics")
@Tag(name = "Public - Statistics")
public class PublicStatisticsController {

    private final DashboardService dashboardService;

    public PublicStatisticsController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/supports-by-type")
    @Operation(
            summary = "Nombre de supports par type de support",
            description = "Répartition des supports de transport par type de support, "
                    + "du plus fréquent au moins fréquent. Indépendant des signalements. "
                    + "Le champ label suit l'en-tête Accept-Language (fr/ar/en) ; labelAr et labelEn "
                    + "exposent toujours les libellés arabes (label_ar) et anglais (label_en)."
    )
    public ResponseEntity<ApiResponse<List<SupportTypeCountResponse>>> supportsByType() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.countSupportsBySupportType()));
    }
}
