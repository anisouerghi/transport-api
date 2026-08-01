package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PermissionMatrixResponse;
import com.transport.reporting.dto.PermissionResponse;
import com.transport.reporting.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@Tag(name = "Admin - Permissions")
public class PermissionController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@perm.hasAny('PERMISSION', 'VIEW', 'SEARCH') or @perm.hasAny('ROLE', 'VIEW', 'EDIT', 'ADD') or @perm.has('USER', 'EDIT')")
    @Operation(summary = "Lister les permissions disponibles")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.findAllPermissions()));
    }

    @GetMapping("/matrix")
    @PreAuthorize("@perm.hasAny('ROLE', 'VIEW', 'EDIT', 'ADD') or @perm.has('PERMISSION', 'VIEW')")
    @Operation(summary = "Matrice modules × actions pour l'affectation aux rôles")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> matrix() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getPermissionMatrix()));
    }
}
