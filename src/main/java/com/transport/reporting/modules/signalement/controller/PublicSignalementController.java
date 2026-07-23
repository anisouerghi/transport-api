package com.transport.reporting.modules.signalement.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.signalement.dto.SignalementRequest;
import com.transport.reporting.modules.signalement.dto.SignalementResponse;
import com.transport.reporting.modules.signalement.service.SignalementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_PUBLIC + "/signalements")
@RequiredArgsConstructor
@Tag(name = "Public - Signalements", description = "Création et suivi des signalements voyageur")
public class PublicSignalementController {

    private final SignalementService signalementService;

    @PostMapping
    @Operation(
            summary = "Créer un signalement",
            description = "Crée une nouvelle réclamation / incident lié à un support transport.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Signalement créé",
                    content = @Content(schema = @Schema(implementation = SignalementResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Support introuvable")
    })
    public ResponseEntity<ApiResponse<SignalementResponse>> create(@Valid @RequestBody SignalementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Signalement créé", signalementService.create(request)));
    }

    @GetMapping("/{reference}")
    @Operation(
            summary = "Consulter le suivi d'un signalement",
            description = "Permet au voyageur de consulter l'état de son signalement via la référence.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Signalement trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Référence inconnue")
    })
    public ResponseEntity<ApiResponse<SignalementResponse>> getByReference(
            @Parameter(description = "Référence du signalement (ex: SIG-20260723-123456)", required = true)
            @PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(signalementService.getByReference(reference)));
    }
}
