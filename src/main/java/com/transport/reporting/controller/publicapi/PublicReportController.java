package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
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

/**
 * Contrôleur public : création et suivi des signalements voyageur.
 * <p>
 * La création accepte un corps {@code multipart/form-data} afin d'envoyer
 * à la fois le DTO JSON ({@code report}) et les fichiers optionnels ({@code files}).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public - Reports")
public class PublicReportController {

    private final ReportService reportService;

    /**
     * Crée un signalement avec pièces jointes optionnelles.
     *
     * @param request données métier du signalement (part JSON {@code report})
     * @param files   fichiers optionnels (part {@code files}, 0 à 5)
     */
    @PostMapping(value = "/api/public/signalements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Créer un signalement (avec pièces jointes optionnelles)",
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
     * Consulte le suivi d'un signalement via sa référence publique.
     */
    @GetMapping("/api/public/suivi/{reference}")
    @Operation(summary = "Consulter le suivi d'une réclamation")
    public ResponseEntity<ApiResponse<ReportResponse>> suivi(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findByReference(reference)));
    }
}
