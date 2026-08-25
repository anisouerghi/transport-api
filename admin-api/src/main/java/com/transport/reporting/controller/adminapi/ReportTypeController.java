package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.ReportTypeCriteria;
import com.transport.reporting.dto.ReportTypeRequest;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.service.ReportTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/report-types")
@Tag(name = "Admin - Report Types")
public class ReportTypeController {

    private final ReportTypeService reportTypeService;
    public ReportTypeController(ReportTypeService reportTypeService) {
        this.reportTypeService = reportTypeService;
    }


    @GetMapping
    @PreAuthorize("@perm.has('REPORT_TYPE', 'VIEW')")
    @Operation(summary = "Lister tous les types de signalement")
    public ResponseEntity<ApiResponse<List<ReportTypeResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(reportTypeService.findAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'VIEW') or @perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les types de signalement actifs")
    public ResponseEntity<ApiResponse<List<ReportTypeResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(reportTypeService.findAllActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'VIEW')")
    @Operation(summary = "Recuperer un type de signalement par id")
    public ResponseEntity<ApiResponse<ReportTypeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportTypeService.findById(id)));
    }

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('REPORT_TYPE', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginee multicritere des types de signalement")
    public ResponseEntity<ApiResponse<PageResponse<ReportTypeResponse>>> search(
            @RequestBody SearchRequest<ReportTypeCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(reportTypeService.search(request)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('REPORT_TYPE', 'ADD')")
    @Operation(summary = "Creer un type de signalement")
    public ResponseEntity<ApiResponse<ReportTypeResponse>> create(
            @Valid @RequestBody ReportTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report type created", reportTypeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'EDIT')")
    @Operation(summary = "Modifier un type de signalement")
    public ResponseEntity<ApiResponse<ReportTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ReportTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Report type updated", reportTypeService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'ACTIVATE')")
    @Operation(summary = "Activer un type de signalement")
    public ResponseEntity<ApiResponse<ReportTypeResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Report type activated", reportTypeService.setActive(id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'DEACTIVATE')")
    @Operation(summary = "Desactiver un type de signalement")
    public ResponseEntity<ApiResponse<ReportTypeResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Report type deactivated", reportTypeService.setActive(id, false)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('REPORT_TYPE', 'DELETE')")
    @Operation(summary = "Supprimer un type de signalement")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
