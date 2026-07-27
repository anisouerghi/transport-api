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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur admin : CRUD supports de transport + gestion QR.
 * Base URL : {@code /api/admin/transport-supports}
 */
@RestController
@RequestMapping("/api/admin/transport-supports")
@RequiredArgsConstructor
@Tag(name = "Admin - Transport Supports")
public class TransportSupportController {

    private final TransportSupportService transportSupportService;

    /** GET / — liste complete. */
    @GetMapping
    @Operation(summary = "Lister tous les supports de transport")
    public ResponseEntity<ApiResponse<List<TransportSupportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.findAll()));
    }

    /** GET /{id} — detail d'un support. */
    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un support par id")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.findById(id)));
    }

    /**
     * POST /search — recherche paginee multicritere.
     * Filtres : reference, label, uuid, qrStatus, supportStatus, supportTypeId, plages de dates.
     */
    @PostMapping("/search")
    @Operation(summary = "Recherche paginee multicritere des supports")
    public ResponseEntity<ApiResponse<PageResponse<TransportSupportResponse>>> search(
            @RequestBody SearchRequest<TransportSupportCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.search(request)));
    }

    /**
     * POST / — creation.
     * Le QR Code (URL + image) est genere automatiquement cote serveur.
     */
    @PostMapping
    @Operation(summary = "Creer un support de transport (QR genere automatiquement)")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> create(
            @Valid @RequestBody TransportSupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transport support created", transportSupportService.create(request)));
    }

    /** PUT /{id} — modification des champs metier (pas de regeneration QR). */
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un support de transport")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransportSupportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transport support updated",
                transportSupportService.update(id, request)));
    }

    /** POST /{id}/generate-qr — regenere l'image et l'URL du QR. */
    @PostMapping("/{id}/generate-qr")
    @Operation(summary = "Regenerer le QR Code d'un support")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> regenerateQr(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("QR code regenerated",
                transportSupportService.regenerateQr(id)));
    }

    /** GET /{id}/qr — telecharge l'image PNG (Content-Type: image/png). */
    @GetMapping("/{id}/qr")
    @Operation(summary = "Telecharger l'image QR Code d'un support")
    public ResponseEntity<byte[]> getQrImage(@PathVariable Long id) {
        byte[] image = transportSupportService.getQrImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qr-" + id + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    /** DELETE /{id} — suppression (HTTP 204). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un support de transport")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transportSupportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
