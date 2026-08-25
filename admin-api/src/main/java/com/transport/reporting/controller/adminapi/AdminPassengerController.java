package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.PassengerCriteria;
import com.transport.reporting.dto.PassengerResponse;
import com.transport.reporting.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur admin : voyageurs (consultation, recherche, activation).
 */
@RestController
@RequestMapping("/api/admin/passengers")
@Tag(name = "Admin - Passengers")
public class AdminPassengerController {

    private final PassengerService passengerService;
    public AdminPassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }


    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('PASSENGER', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginée des voyageurs")
    public ResponseEntity<ApiResponse<PageResponse<PassengerResponse>>> search(
            @RequestBody SearchRequest<PassengerCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(passengerService.search(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('PASSENGER', 'VIEW')")
    @Operation(summary = "Détail d'un voyageur")
    public ResponseEntity<ApiResponse<PassengerResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(passengerService.findById(id)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@perm.has('PASSENGER', 'ACTIVATE')")
    @Operation(summary = "Activer un voyageur")
    public ResponseEntity<ApiResponse<PassengerResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Passenger activated", passengerService.setActive(id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('PASSENGER', 'DEACTIVATE')")
    @Operation(summary = "Désactiver un voyageur")
    public ResponseEntity<ApiResponse<PassengerResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Passenger deactivated", passengerService.setActive(id, false)));
    }
}
