package com.transport.reporting.controller.adminapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.AttachmentResponse;
import com.transport.reporting.entity.Attachment;
import com.transport.reporting.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Contrôleur admin dédié aux pièces jointes des signalements.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Attachments")
public class AdminAttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/api/admin/signalements/{reportId}/attachments")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Lister les pièces jointes d'un signalement")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> listByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(attachmentService.findByReportId(reportId)));
    }

    @GetMapping("/api/admin/attachments/{id}/download")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Télécharger une pièce jointe")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.getEntity(id);
        byte[] content = attachmentService.readContent(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("attachment", attachment.getFileName()))
                .contentType(resolveMediaType(attachment.getFileType()))
                .body(content);
    }

    /**
     * Sert le fichier pour affichage navigateur (en-tête {@code inline}).
     */
    @GetMapping("/api/admin/attachments/{id}/view")
    @PreAuthorize("@perm.has('REPORT', 'VIEW')")
    @Operation(summary = "Afficher une pièce jointe (inline)")
    public ResponseEntity<byte[]> view(@PathVariable Long id) {
        Attachment attachment = attachmentService.getEntity(id);
        byte[] content = attachmentService.readContent(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("inline", attachment.getFileName()))
                .contentType(resolveMediaType(attachment.getFileType()))
                .body(content);
    }

    /** Convertit le MIME stocké en {@link MediaType}, avec repli octet-stream. */
    private MediaType resolveMediaType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(fileType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Construit l'en-tête Content-Disposition compatible ASCII + UTF-8 (RFC 5987).
     */
    private String contentDisposition(String type, String fileName) {
        String safe = fileName == null ? "file" : fileName.replace("\"", "");
        String encoded = java.net.URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        return type + "; filename=\"" + safe + "\"; filename*=UTF-8''" + encoded;
    }
}
