package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.ReplyCreateResult;
import com.transport.reporting.dto.ReplyRequest;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/admin/reports/{reportId}/replies")
@RequiredArgsConstructor
@Tag(name = "Admin - Report Replies")
public class AdminReplyController {

    private final ReplyService replyService;

    @GetMapping
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les reponses d'un signalement")
    public ResponseEntity<ApiResponse<List<ReplyResponse>>> findByReportId(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(replyService.findByReportId(reportId)));
    }

    @PostMapping
    @PreAuthorize("@perm.has('REPORT', 'REPLY')")
    @Operation(
            summary = "Créer une réponse sur un signalement",
            description = "Enregistre la réponse agent. "
                    + "Si sendEmail=true, tente l'envoi SMTP. "
                    + "En cas d'échec e-mail : success=false, errorCode renseigné, data contient quand même la réponse enregistrée."
    )
    public ResponseEntity<ApiResponse<ReplyResponse>> create(
            @PathVariable Long reportId,
            @Valid @RequestBody ReplyRequest request) {
        ReplyCreateResult result = replyService.create(reportId, request);
        HttpStatus status = result.isReplySaved() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(ApiResponse.of(
                result.isSuccess(),
                result.getMessage(),
                result.getErrorCode(),
                result.getReply()));
    }
}
