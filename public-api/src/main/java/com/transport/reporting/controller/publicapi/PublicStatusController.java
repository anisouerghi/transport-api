package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur public exposant la liste des statuts de workflow.
 *
 * <p>Reprise à l'identique de {@code GET /api/admin/status} : mêmes données
 * ({@link StatusResponse}), même service, même enveloppe {@link ApiResponse}.
 * Seule la racine diffère ({@code /api/public} au lieu de {@code /api/admin}),
 * aucun droit n'est requis — cette vue ne fait qu'exposer le catalogue de
 * statuts déjà visible dans {@code PublicReportTrackingResponse.statusLabel}.
 *
 * <p>Les points d'écriture de l'équivalent admin (POST/PUT/DELETE) ne sont
 * volontairement pas recopiés : ils sont protégés par {@code @PreAuthorize}
 * côté admin et ne doivent jamais être exposés en accès libre.
 */
@RestController
@RequestMapping("/api/public/status")
@Tag(name = "Public - Status")
public class PublicStatusController {

    private final StatusService statusService;

    public PublicStatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    @Operation(summary = "Lister tous les statuts")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findAll()));
    }
}
