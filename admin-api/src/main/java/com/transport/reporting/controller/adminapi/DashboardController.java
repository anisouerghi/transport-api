package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.DashboardResponse;
import com.transport.reporting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin - Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }


    @GetMapping
    @PreAuthorize("@perm.has('DASHBOARD', 'VIEW')")
    @Operation(summary = "Consulter le tableau de bord")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard()));
    }
}
