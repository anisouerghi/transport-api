package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.AuditLogCriteria;
import com.transport.reporting.dto.AuditLogResponse;
import com.transport.reporting.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('AUDIT', 'SEARCH', 'VIEW')")
    @Operation(summary = "Recherche paginée multicritère du journal d'audit")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
            @RequestBody SearchRequest<AuditLogCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.search(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('AUDIT', 'VIEW')")
    @Operation(summary = "Consulter le détail d'une entrée d'audit")
    public ResponseEntity<ApiResponse<AuditLogResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.findById(id)));
    }
}
