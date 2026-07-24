package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur public : creation et suivi des signalements voyageur.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public - Reports")
public class PublicReportController {

    private final ReportService reportService;

    @PostMapping("/api/public/signalements")
    @Operation(summary = "Créer un signalement")
    public ResponseEntity<ApiResponse<ReportResponse>> create(@Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report created", reportService.create(request)));
    }

    @GetMapping("/api/public/suivi/{reference}")
    @Operation(summary = "Consulter le suivi d'une réclamation")
    public ResponseEntity<ApiResponse<ReportResponse>> suivi(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findByReference(reference)));
    }
}
