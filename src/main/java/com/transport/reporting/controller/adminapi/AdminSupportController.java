package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.TransportSupportRequest;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur admin : gestion des supports de transport.
 */
@RestController
@RequestMapping("/api/admin/supports")
@RequiredArgsConstructor
@Tag(name = "Admin - Supports")
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping
    @Operation(summary = "Lister les supports")
    public ResponseEntity<ApiResponse<List<TransportSupportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(supportService.findAll()));
    }

    @PostMapping
    @Operation(summary = "Créer un support")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> create(
            @Valid @RequestBody TransportSupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support created", supportService.create(request)));
    }
}
