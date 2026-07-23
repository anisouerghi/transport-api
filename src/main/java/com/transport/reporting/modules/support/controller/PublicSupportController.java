package com.transport.reporting.modules.support.controller;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.support.dto.TransportSupportResponse;
import com.transport.reporting.modules.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/supports")
@RequiredArgsConstructor
@Tag(name = "Public - Supports")
public class PublicSupportController {

    private final SupportService supportService;

    @GetMapping("/{uuid}")
    @Operation(summary = "Identifier un support via UUID du QR Code")
    public ResponseEntity<ApiResponse<TransportSupportResponse>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.findActiveByUuid(uuid)));
    }
}
