package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.config.GoogleOAuthProperties;
import com.transport.reporting.dto.GoogleCallbackRequest;
import com.transport.reporting.dto.PassengerAuthResponse;
import com.transport.reporting.dto.PassengerLoginRequest;
import com.transport.reporting.dto.PassengerRegisterRequest;
import com.transport.reporting.security.GoogleOAuth2LoginSuccessHandler;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.security.GoogleOAuthCallbackCodeStore;
import com.transport.reporting.service.PassengerAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * API publique d'authentification voyageur (après scan QR).
 * Distincte de {@code /api/auth} (agents admin).
 */
@RestController
@RequestMapping("/api/public/auth")
@Tag(name = "Public - Auth Voyageur")
public class PublicPassengerAuthController {

    private final PassengerAuthService passengerAuthService;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleOAuthCallbackCodeStore callbackCodeStore;

    public PublicPassengerAuthController(
            PassengerAuthService passengerAuthService,
            GoogleOAuthProperties googleOAuthProperties,
            ObjectProvider<GoogleOAuthCallbackCodeStore> callbackCodeStore) {
        this.passengerAuthService = passengerAuthService;
        this.googleOAuthProperties = googleOAuthProperties;
        this.callbackCodeStore = callbackCodeStore.getIfAvailable();
    }


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

    @GetMapping("/google")
    @Operation(summary = "Démarrer la connexion Google OAuth 2.0 / OIDC")
    public void startGoogleLogin(
            @RequestParam(required = false) String returnUrl,
            HttpServletResponse response) throws IOException {
        if (!googleOAuthProperties.isConfigured()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(
                    "Connexion Google non configurée sur le serveur. "
                            + "Définissez GOOGLE_CLIENT_SECRET puis redémarrez public-api.");
            return;
        }

        if (StringUtils.hasText(returnUrl)) {
            Cookie cookie = new Cookie(
                    GoogleOAuth2LoginSuccessHandler.RETURN_URL_COOKIE,
                    GoogleOAuth2LoginSuccessHandler.sanitizeReturnUrl(returnUrl));
            cookie.setPath("/");
            cookie.setMaxAge(600);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/google/callback")
    @Operation(summary = "Échanger un code OAuth éphémère contre un JWT voyageur")
    public ResponseEntity<ApiResponse<PassengerAuthResponse>> completeGoogleLogin(
            @Valid @RequestBody GoogleCallbackRequest request) {
        if (callbackCodeStore == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.of(false, "Connexion Google non configurée.", "GOOGLE_NOT_CONFIGURED", null));
        }

        return callbackCodeStore.redeem(request.getCode())
                .map(auth -> ResponseEntity.ok(ApiResponse.ok(auth)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.of(false, "Code Google invalide ou expiré.", "GOOGLE_CODE_INVALID", null)));
    }
}
