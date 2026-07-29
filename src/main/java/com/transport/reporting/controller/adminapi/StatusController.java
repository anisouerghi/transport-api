package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.StatusRequest;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur Admin : CRUD des statuts.
 */
@RestController
@RequestMapping("/api/admin/status")
@RequiredArgsConstructor
@Tag(name = "Admin - Status", description = "Gestion des statuts")
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    @Operation(summary = "Lister tous les statuts")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un statut par son ID")
    public ResponseEntity<ApiResponse<StatusResponse>> findById(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Créer un statut")
    public ResponseEntity<ApiResponse<StatusResponse>> create(
            @Valid @RequestBody StatusRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Status created", statusService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un statut")
    public ResponseEntity<ApiResponse<StatusResponse>> update(
            @PathVariable long  id,
            @Valid @RequestBody StatusRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Status updated", statusService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un statut")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        statusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}