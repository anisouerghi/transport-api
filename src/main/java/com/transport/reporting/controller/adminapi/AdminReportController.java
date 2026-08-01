package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.service.ReportService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/signalements")
@RequiredArgsConstructor
@Tag(name = "Admin - Reports")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les signalements")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findAll()));
    }

    @PostMapping("/search")
    @PreAuthorize("@perm.hasAny('REPORT', 'SEARCH', 'VIEW')")
    @Operation(summary = "Rechercher les signalements selon des critères")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> search(
            @RequestBody SearchRequest<ReportCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.search(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Consulter un signalement")
    public ResponseEntity<ApiResponse<ReportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findById(id)));
    }
}
