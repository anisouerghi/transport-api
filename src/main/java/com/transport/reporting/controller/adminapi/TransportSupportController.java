package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.TransportSupportCriteria;
import com.transport.reporting.dto.TransportSupportRequest;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.service.TransportSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/transport-supports")
@RequiredArgsConstructor
@Tag(name = "Admin - Transport Supports")
public class TransportSupportController {

    private final TransportSupportService transportSupportService;

    @GetMapping
    @PreAuthorize("@perm.has('TRANSPORT_SUPPORT', 'VIEW')")
    @Operation(summary = "Lister tous les supports de transport")
    public ResponseEntity<ApiResponse<List<TransportSupportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('TRANSPORT_SUPPORT', 'VIEW')")
    @Operation(summary = "Recuperer un support par id")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.findById(id)));
    }

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('TRANSPORT_SUPPORT', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginee multicritere des supports")
    public ResponseEntity<ApiResponse<PageResponse<TransportSupportResponse>>> search(
            @RequestBody SearchRequest<TransportSupportCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.search(request)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('TRANSPORT_SUPPORT', 'ADD')")
    @Operation(summary = "Creer un support de transport (QR genere automatiquement)")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> create(
            @Valid @RequestBody TransportSupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transport support created", transportSupportService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('TRANSPORT_SUPPORT', 'EDIT')")
    @Operation(summary = "Modifier un support de transport")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransportSupportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transport support updated",
                transportSupportService.update(id, request)));
    }

    @PostMapping("/{id}/generate-qr")
    @PreAuthorize("@perm.hasAny('TRANSPORT_SUPPORT', 'EDIT', 'PRINT')")
    @Operation(summary = "Regenerer le QR Code d'un support")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> regenerateQr(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("QR code regenerated",
                transportSupportService.regenerateQr(id)));
    }

    @PostMapping("/generate-qr")
    @PreAuthorize("@perm.hasAny('TRANSPORT_SUPPORT', 'EDIT', 'PRINT')")
    @Operation(summary = "Regenerer le QR Code de tous les supports")
    public ResponseEntity<ApiResponse<List<TransportSupportResponse>>> regenerateQrAll() {
        return ResponseEntity.ok(ApiResponse.ok("All QR codes regenerated",
                transportSupportService.regenerateQrAll()));
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("@perm.hasAny('TRANSPORT_SUPPORT', 'PRINT', 'VIEW')")
    @Operation(summary = "Telecharger l'image QR Code d'un support")
    public ResponseEntity<byte[]> getQrImage(@PathVariable Long id) {
        byte[] image = transportSupportService.getQrImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qr-" + id + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('TRANSPORT_SUPPORT', 'DELETE')")
    @Operation(summary = "Supprimer un support de transport")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transportSupportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
