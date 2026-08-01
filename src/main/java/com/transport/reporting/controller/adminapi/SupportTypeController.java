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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-types")
@RequiredArgsConstructor
@Tag(name = "Admin - Support Types")
public class SupportTypeController {

    private final SupportTypeService supportTypeService;

    @GetMapping
    @PreAuthorize("@perm.has('SUPPORT_TYPE', 'VIEW')")
    @Operation(summary = "Lister tous les types de support")
    public ResponseEntity<ApiResponse<List<SupportTypeResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('SUPPORT_TYPE', 'VIEW')")
    @Operation(summary = "Recuperer un type de support par id")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.findById(id)));
    }

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('SUPPORT_TYPE', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginee multicritere des types de support")
    public ResponseEntity<ApiResponse<PageResponse<SupportTypeResponse>>> search(
            @RequestBody SearchRequest<SupportTypeCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(supportTypeService.search(request)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('SUPPORT_TYPE', 'ADD')")
    @Operation(summary = "Creer un type de support")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> create(
            @Valid @RequestBody SupportTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support type created", supportTypeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SUPPORT_TYPE', 'EDIT')")
    @Operation(summary = "Modifier un type de support")
    public ResponseEntity<ApiResponse<SupportTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Support type updated", supportTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SUPPORT_TYPE', 'DELETE')")
    @Operation(summary = "Supprimer un type de support")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supportTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
