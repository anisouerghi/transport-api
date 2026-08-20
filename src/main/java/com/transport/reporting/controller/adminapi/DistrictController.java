package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.DistrictCriteria;
import com.transport.reporting.dto.DistrictRequest;
import com.transport.reporting.dto.DistrictResponse;
import com.transport.reporting.service.DistrictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/districts")
@RequiredArgsConstructor
@Tag(name = "Admin - Districts")
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Lister tous les districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> findAll() {
        try {
            log.info("GET /api/admin/districts - findAll()");
            List<DistrictResponse> districts = districtService.findAll();
            log.info("Found {} districts", districts.size());
            return ResponseEntity.ok(ApiResponse.ok(districts));
        } catch (Exception e) {
            log.error("Error in findAll()", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Recuperer un district par id")
    public ResponseEntity<ApiResponse<DistrictResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(districtService.findById(id)));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Recherche paginee multicritere des districts")
    public ResponseEntity<ApiResponse<PageResponse<DistrictResponse>>> search(
            @RequestBody SearchRequest<DistrictCriteria> request) {
        return ResponseEntity.ok(ApiResponse.ok(districtService.search(request)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Creer un district")
    public ResponseEntity<ApiResponse<DistrictResponse>> create(
            @Valid @RequestBody DistrictRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("District created", districtService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Modifier un district")
    public ResponseEntity<ApiResponse<DistrictResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody DistrictRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("District updated", districtService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Supprimer un district")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        districtService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
