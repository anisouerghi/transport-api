package com.transport.reporting.modules.support.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.modules.support.dto.SupportRequest;
import com.transport.reporting.modules.support.dto.SupportResponse;
import com.transport.reporting.modules.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_ADMIN + "/supports")
@RequiredArgsConstructor
@Tag(name = "Admin - Supports")
public class AdminSupportController {

    private final SupportService supportService;

    @PostMapping
    @Operation(summary = "Créer un support de transport")
    public ResponseEntity<ApiResponse<SupportResponse>> create(@Valid @RequestBody SupportRequest request) {
        SupportResponse created = supportService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support créé", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un support")
    public ResponseEntity<ApiResponse<SupportResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un support")
    public ResponseEntity<ApiResponse<SupportResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Lister les supports")
    public ResponseEntity<ApiResponse<PageResponse<SupportResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(supportService.findAll(pageable))));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activer un support")
    public ResponseEntity<ApiResponse<SupportResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Support activé", supportService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un support")
    public ResponseEntity<ApiResponse<SupportResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Support désactivé", supportService.deactivate(id)));
    }
}
