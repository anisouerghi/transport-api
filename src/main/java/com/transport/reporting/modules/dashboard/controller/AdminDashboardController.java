package com.transport.reporting.modules.dashboard.controller;

import com.transport.reporting.common.constants.AppConstants;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.dashboard.dto.DashboardStatsResponse;
import com.transport.reporting.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_ADMIN + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Consulter les indicateurs du tableau de bord")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats()));
    }
}
