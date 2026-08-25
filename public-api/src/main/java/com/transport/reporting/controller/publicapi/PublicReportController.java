package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PublicReportListItemResponse;
import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.service.PublicTrackingService;
import com.transport.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur public : création et suivi des signalements voyageur.
 */
@RestController
@Tag(name = "Public - Reports")
public class PublicReportController {

    private final ReportService reportService;
    private final PublicTrackingService publicTrackingService;
    public PublicReportController(ReportService reportService, PublicTrackingService publicTrackingService) {
        this.reportService = reportService;
        this.publicTrackingService = publicTrackingService;
    }


    @PostMapping(value = "/api/public/signalements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Créer un signalement (avec pièces jointes optionnelles)",
            description = "La priorité n'est pas acceptée côté voyageur : elle est initialisée automatiquement (MEDIUM) et gérée ensuite par les agents.",
            requestBody = @RequestBody(content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = {
                            @Encoding(name = "report", contentType = MediaType.APPLICATION_JSON_VALUE),
                            @Encoding(name = "files", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    }
            ))
    )
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @Valid @RequestPart("report")
            @Schema(implementation = ReportRequest.class) ReportRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report created", reportService.create(request, files)));
    }

    @GetMapping("/api/public/signalements/mine")
    @Operation(
            summary = "Lister mes 15 derniers signalements",
            description = "Réservé au voyageur authentifié. L'identité vient uniquement du JWT : "
                    + "aucun identifiant voyageur n'est accepté en paramètre. Filtre optionnel par référence."
    )
    public ResponseEntity<ApiResponse<List<PublicReportListItemResponse>>> mine(
            @AuthenticationPrincipal PassengerPrincipal principal,
            @RequestParam(required = false) String reference) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.of(false, "Authentification requise.", "AUTH_REQUIRED", null));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                publicTrackingService.listMine(principal.getPassengerId(), reference)));
    }

    /**
     * Suivi sécurisé par UUID (lien e-mail). N'expose que les réponses publiques.
     */
    @GetMapping("/api/public/signalements/{uuid}/follow-up")
    @Operation(
            summary = "Suivi sécurisé d'un signalement (UUID)",
            description = "Accès public via lien e-mail (UUID non prévisible). "
                    + "Retourne uniquement les informations destinées au voyageur et les réponses visibles."
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> followUpByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.findByUuid(uuid)));
    }

    /**
     * @deprecated Préférer {@link #followUpByUuid(UUID)} — conservé pour compatibilité.
     */
    @Deprecated
    @GetMapping("/api/public/suivi/{uuid}")
    @Operation(
            summary = "[Déprécié] Consulter le suivi d'un signalement par UUID",
            description = "Alias de GET /api/public/signalements/{uuid}/follow-up"
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> suiviByUuid(@PathVariable UUID uuid) {
        return followUpByUuid(uuid);
    }
}
