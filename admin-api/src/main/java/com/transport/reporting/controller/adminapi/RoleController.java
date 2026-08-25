package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PermissionMatrixResponse;
import com.transport.reporting.dto.PermissionResponse;
import com.transport.reporting.dto.RoleRequest;
import com.transport.reporting.dto.RoleResponse;
import com.transport.reporting.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@Tag(name = "Admin - Roles")
public class RoleController {

    private final RoleService roleService;
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }


    @GetMapping
    @PreAuthorize("@perm.has('ROLE', 'VIEW')")
    @Operation(summary = "Lister les rôles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('ROLE', 'VIEW')")
    @Operation(summary = "Détail d'un rôle")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('ROLE', 'ADD')")
    @Operation(summary = "Créer un rôle")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Role created", roleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('ROLE', 'EDIT')")
    @Operation(summary = "Modifier un rôle et ses permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Role updated", roleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ROLE', 'DELETE')")
    @Operation(summary = "Supprimer un rôle")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
