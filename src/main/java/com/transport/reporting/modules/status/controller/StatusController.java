package com.transport.reporting.modules.status.controller;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.modules.status.dto.StatusResponse;
import com.transport.reporting.modules.status.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statuses")
@RequiredArgsConstructor
@Tag(name = "Admin - Status")
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    @Operation(summary = "Lister les statuts")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(statusService.findAll()));
    }
}
