package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.ReportNatureCriteria;
import com.transport.reporting.dto.ReportNatureRequest;
import com.transport.reporting.dto.ReportNatureResponse;
import com.transport.reporting.service.ReportNatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/natures")
@Tag(name = "Admin - Report Natures")
public class ReportNatureController {

    private final ReportNatureService reportNatureService;

    public ReportNatureController(ReportNatureService reportNatureService) {
        this.reportNatureService = reportNatureService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('NATURE', 'VIEW')")
    @Operation(summary = "Lister toutes les natures")
    public ResponseEntity<ApiResponse<List<ReportNatureResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(reportNatureService.findAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("@perm.has('NATURE', 'VIEW') or @perm.has('REPORT', 'ASSIGN_NATURE') or @perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les natures actives (affectation / filtres)")
    public ResponseEntity<ApiResponse<List<ReportNatureResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(reportNatureService.findAllActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('NATURE', 'VIEW')")
    @Operation(summary = "Récupérer une nature par id")
    public ResponseEntity<ApiResponse<ReportNatureResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportNatureService.findById(id)));
    }

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('NATURE', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginée des natures")
    public ResponseEntity<ApiResponse<PageResponse<ReportNatureResponse>>> search(
            @RequestBody SearchRequest<ReportNatureCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(reportNatureService.search(request)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('NATURE', 'ADD')")
    @Operation(summary = "Créer une nature")
    public ResponseEntity<ApiResponse<ReportNatureResponse>> create(
            @Valid @RequestBody ReportNatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Nature created", reportNatureService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('NATURE', 'EDIT')")
    @Operation(summary = "Modifier une nature")
    public ResponseEntity<ApiResponse<ReportNatureResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ReportNatureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Nature updated", reportNatureService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@perm.has('NATURE', 'ACTIVATE')")
    @Operation(summary = "Activer une nature")
    public ResponseEntity<ApiResponse<ReportNatureResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Nature activated", reportNatureService.setActive(id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('NATURE', 'DEACTIVATE')")
    @Operation(summary = "Désactiver une nature")
    public ResponseEntity<ApiResponse<ReportNatureResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Nature deactivated", reportNatureService.setActive(id, false)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('NATURE', 'DELETE')")
    @Operation(summary = "Supprimer une nature (interdit si utilisée)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportNatureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
