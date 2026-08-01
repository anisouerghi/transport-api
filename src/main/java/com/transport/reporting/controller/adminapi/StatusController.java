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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/status")
@RequiredArgsConstructor
@Tag(name = "Admin - Status")
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    @PreAuthorize("@perm.has('STATUS', 'VIEW') or @perm.hasAny('REPORT', 'VIEW', 'REPLY')")
    @Operation(summary = "Lister tous les statuts")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('STATUS', 'VIEW')")
    @Operation(summary = "Récupérer un statut par son ID")
    public ResponseEntity<ApiResponse<StatusResponse>> findById(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('STATUS', 'ADD')")
    @Operation(summary = "Créer un statut")
    public ResponseEntity<ApiResponse<StatusResponse>> create(@Valid @RequestBody StatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Status created", statusService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('STATUS', 'EDIT')")
    @Operation(summary = "Modifier un statut")
    public ResponseEntity<ApiResponse<StatusResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", statusService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('STATUS', 'DELETE')")
    @Operation(summary = "Supprimer un statut")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        statusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
