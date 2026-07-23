package com.transport.reporting.modules.signalement.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.modules.signalement.dto.*;
import com.transport.reporting.modules.signalement.service.SignalementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_ADMIN + "/signalements")
@RequiredArgsConstructor
@Tag(name = "Admin - Signalements")
public class AdminSignalementController {

    private final SignalementService signalementService;

    @GetMapping
    @Operation(summary = "Recherche multicritère des signalements")
    public ResponseEntity<ApiResponse<PageResponse<SignalementResponse>>> search(
            SignalementSearchDTO criteria,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(signalementService.search(criteria, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un signalement")
    public ResponseEntity<ApiResponse<SignalementResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(signalementService.getById(id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'un signalement")
    public ResponseEntity<ApiResponse<SignalementResponse>> changeStatut(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(signalementService.changeStatut(id, request)));
    }

    @PutMapping("/{id}/affectation")
    @Operation(summary = "Affecter un signalement à un service")
    public ResponseEntity<ApiResponse<SignalementResponse>> affecter(
            @PathVariable Long id,
            @Valid @RequestBody AffectationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(signalementService.affecter(id, request)));
    }

    @PutMapping("/{id}/reponse")
    @Operation(summary = "Répondre au voyageur")
    public ResponseEntity<ApiResponse<SignalementResponse>> repondre(
            @PathVariable Long id,
            @Valid @RequestBody ReponseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(signalementService.repondre(id, request)));
    }
}
