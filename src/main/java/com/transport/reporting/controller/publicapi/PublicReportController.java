package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.service.PublicTrackingService;
import com.transport.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Contrôleur public : création et suivi des signalements voyageur.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public - Reports")
public class PublicReportController {

    private final ReportService reportService;
    private final PublicTrackingService publicTrackingService;

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

    /**
     * Suivi sécurisé par UUID (lien e-mail). N'expose que les réponses publiques.
     */
    @GetMapping("/api/public/suivi/{uuid}")
    @Operation(
            summary = "Consulter le suivi d'un signalement par UUID",
            description = "Accès public sécurisé via UUID (non prévisible). "
                    + "Retourne uniquement les réponses marquées visibles pour l'auteur. "
                    + "N'expose pas les priorités, agents ni IDs internes."
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> suiviByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.findByUuid(uuid)));
    }
}
