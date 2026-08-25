package com.transport.reporting.controller;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.LoginRequest;
import com.transport.reporting.dto.LoginResponse;
import com.transport.reporting.dto.UpdatePasswordRequest;
import com.transport.reporting.security.AuthenticationService;
import com.transport.reporting.security.UserPrincipal;
import com.transport.reporting.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API d'authentification (login / profil courant).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion et génération du JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authenticationService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur authentifié", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LoginResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authenticationService.currentUser(principal)));
    }

    @PostMapping("/update-password")
    @Operation(summary = "Mettre à jour le mot de passe de l'utilisateur connecté", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe mis à jour avec succès", null));
    }
}
