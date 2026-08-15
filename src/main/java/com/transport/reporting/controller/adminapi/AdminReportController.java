package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.dto.UpdatePriorityRequest;
import com.transport.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/signalements")
@Tag(name = "Admin - Reports")
public class AdminReportController {

    private final ReportService reportService;
    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }


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

    @GetMapping("/priorities")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les priorités disponibles")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> priorities() {
        List<Map<String, String>> values = Arrays.stream(Priority.values())
                .map(p -> Map.of("code", p.name(), "label", priorityLabel(p)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(values));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Consulter un signalement")
    public ResponseEntity<ApiResponse<ReportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findById(id)));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("@perm.has('REPORT', 'UPDATE_PRIORITY')")
    @Operation(summary = "Modifier la priorité d'un signalement (traitement interne)")
    public ResponseEntity<ApiResponse<ReportResponse>> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Priority updated",
                reportService.updatePriority(id, request.getPriority())));
    }

    private static String priorityLabel(Priority priority) {
        return switch (priority) {
            case LOW -> "Faible";
            case MEDIUM -> "Normale";
            case HIGH -> "Élevée";
            case CRITICAL -> "Critique";
        };
    }
}
