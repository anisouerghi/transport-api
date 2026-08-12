package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PassengerAuthResponse;
import com.transport.reporting.dto.PassengerLoginRequest;
import com.transport.reporting.dto.PassengerRegisterRequest;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.service.PassengerAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API publique d'authentification voyageur (après scan QR).
 * Distincte de {@code /api/auth} (agents admin).
 */
@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
@Tag(name = "Public - Auth Voyageur")
public class PublicPassengerAuthController {

    private final PassengerAuthService passengerAuthService;

    @PostMapping("/register")
    @Operation(summary = "Créer un compte voyageur")
    public ResponseEntity<ApiResponse<PassengerAuthResponse>> register(
            @Valid @RequestBody PassengerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compte créé", passengerAuthService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion voyageur")
    public ResponseEntity<ApiResponse<PassengerAuthResponse>> login(
            @Valid @RequestBody PassengerLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(passengerAuthService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil voyageur connecté", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PassengerAuthResponse>> me(
            @AuthenticationPrincipal PassengerPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.of(false, "Authentification requise.", "AUTH_REQUIRED", null));
        }
        return ResponseEntity.ok(ApiResponse.ok(passengerAuthService.current(principal)));
    }
}
