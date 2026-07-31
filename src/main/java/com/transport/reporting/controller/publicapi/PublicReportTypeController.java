package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.service.ReportTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur public exposant les types de signalement actifs
 * pour le formulaire voyageur (scan QR).
 */
@RestController
@RequestMapping("/api/public/report-types")
@RequiredArgsConstructor
@Tag(name = "Public - Report Types")
public class PublicReportTypeController {

    private final ReportTypeService reportTypeService;

    /**
     * Liste uniquement les types dont le flag {@code active} est vrai.
     */
    @GetMapping
    @Operation(summary = "Lister les types de signalement actifs")
    public ResponseEntity<ApiResponse<List<ReportTypeResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(reportTypeService.findAllActive()));
    }
}
