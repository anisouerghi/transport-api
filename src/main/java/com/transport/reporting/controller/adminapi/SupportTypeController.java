package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.SupportTypeCriteria;
import com.transport.reporting.dto.SupportTypeRequest;
import com.transport.reporting.dto.SupportTypeResponse;
import com.transport.reporting.service.SupportTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur admin : CRUD types de support.
 * Base URL : {@code /api/admin/support-types}
 */
@RestController
@RequestMapping("/api/admin/support-types")
@RequiredArgsConstructor
@Tag(name = "Admin - Support Types")
public class SupportTypeController {

    private final SupportTypeService supportTypeService;

    /** GET / — liste complete (dropdowns frontend). */
    @GetMapping
    @Operation(summary = "Lister tous les types de support")
    public ResponseEntity<ApiResponse<List<SupportTypeResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.findAll()));
    }

    /** GET /{id} — consultation par id. */
    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un type de support par id")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.findById(id)));
    }

    /**
     * POST /search — recherche paginee.
     * Body : {@code { "filters": { "code", "label" }, "pageable": { ... } }}
     */
    @PostMapping("/search")
    @Operation(summary = "Recherche paginee multicritere des types de support")
    public ResponseEntity<ApiResponse<PageResponse<SupportTypeResponse>>> search(
            @RequestBody SearchRequest<SupportTypeCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.search(request)));
    }

    /** POST / — creation (HTTP 201). */
    @PostMapping
    @Operation(summary = "Creer un type de support")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> create(
            @Valid @RequestBody SupportTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support type created", supportTypeService.create(request)));
    }

    /** PUT /{id} — modification. */
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un type de support")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Support type updated", supportTypeService.update(id, request)));
    }

    /** DELETE /{id} — suppression (HTTP 204, pas de body). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un type de support")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supportTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
