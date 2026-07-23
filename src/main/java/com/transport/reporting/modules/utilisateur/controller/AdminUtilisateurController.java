package com.transport.reporting.modules.utilisateur.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurRequest;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurResponse;
import com.transport.reporting.modules.utilisateur.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_ADMIN + "/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Admin - Utilisateurs")
public class AdminUtilisateurController {

    private final UtilisateurService utilisateurService;

    @PostMapping
    @Operation(summary = "Créer un utilisateur interne")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> create(@Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Utilisateur créé", utilisateurService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un utilisateur")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(utilisateurService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs")
    public ResponseEntity<ApiResponse<PageResponse<UtilisateurResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(utilisateurService.findAll(pageable))));
    }
}
