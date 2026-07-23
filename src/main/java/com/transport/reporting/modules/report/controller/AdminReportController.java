package com.transport.reporting.modules.report.controller;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.report.dto.ReportResponse;
import com.transport.reporting.modules.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @Operation(summary = "Lister les signalements")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un signalement")
    public ResponseEntity<ApiResponse<ReportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.findById(id)));
    }
}
