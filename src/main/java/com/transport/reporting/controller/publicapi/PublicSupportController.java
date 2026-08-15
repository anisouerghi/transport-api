package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.service.TransportSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controleur public : identification d'un support via QR Code.
 */
@RestController
@RequestMapping("/api/public/supports")
@Tag(name = "Public - Supports")
public class PublicSupportController {

    private final TransportSupportService transportSupportService;
    public PublicSupportController(TransportSupportService transportSupportService) {
        this.transportSupportService = transportSupportService;
    }


    @GetMapping("/{uuid}")
    @Operation(summary = "Identifier un support via UUID du QR Code")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(transportSupportService.findActiveByUuid(uuid)));
    }
}
