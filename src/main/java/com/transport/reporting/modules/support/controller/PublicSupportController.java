package com.transport.reporting.modules.support.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.support.dto.SupportResponse;
import com.transport.reporting.modules.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(AppConstants.API_PUBLIC + "/supports")
@RequiredArgsConstructor
@Tag(name = "Public - Supports", description = "Identification d'un support via QR Code")
public class PublicSupportController {

    private final SupportService supportService;

    @GetMapping("/{uuid}")
    @Operation(
            summary = "Identifier un support via UUID du QR Code",
            description = "Retourne les informations du support de transport actif associé au QR Code scanné.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Support trouvé",
                    content = @Content(schema = @Schema(implementation = SupportResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Support introuvable ou inactif")
    })
    public ResponseEntity<ApiResponse<SupportResponse>> getByUuid(
            @Parameter(description = "UUID présent dans le QR Code", required = true)
            @PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.getActiveByUuid(uuid)));
    }
}
