package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.ReplyRequest;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controleur admin : reponses agents sur les signalements.
 * Base URL : {@code /api/admin/reports/{reportId}/replies}
 */
@RestController
@RequestMapping("/api/admin/reports/{reportId}/replies")
@RequiredArgsConstructor
@Tag(name = "Admin - Report Replies")
public class AdminReplyController {

    private final ReplyService replyService;

    @GetMapping
    @Operation(summary = "Lister les reponses d'un signalement")
    public ResponseEntity<ApiResponse<List<ReplyResponse>>> findByReportId(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(replyService.findByReportId(reportId)));
    }

    @PostMapping
    @Operation(summary = "Creer une reponse sur un signalement")
    public ResponseEntity<ApiResponse<ReplyResponse>> create(
            @PathVariable Long reportId,
            @Valid @RequestBody ReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reply created", replyService.create(reportId, request)));
    }
}
