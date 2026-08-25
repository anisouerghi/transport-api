package com.transport.reporting.mapper;

import com.transport.reporting.dto.AttachmentResponse;
import com.transport.reporting.entity.Attachment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mapper pièce jointe : conversion {@link Attachment} → {@link AttachmentResponse}.
 */
@Component
public class AttachmentMapper {

    /**
     * Convertit l'entité JPA en DTO d'API, en enrichissant taille et indicateur image.
     */
    public AttachmentResponse toResponse(Attachment attachment) {
        String fileType = attachment.getFileType();
        return AttachmentResponse.builder()
                .attachmentId(attachment.getAttachmentId())
                .uuid(attachment.getUuid())
                .fileName(attachment.getFileName())
                .fileType(fileType)
                .fileSize(resolveFileSize(attachment.getFilePath()))
                .reportId(attachment.getReport() != null ? attachment.getReport().getReportId() : null)
                .image(fileType != null && fileType.startsWith("image/"))
                .build();
    }

    /** Lit la taille du fichier sur disque ; retourne null si inaccessible. */
    private Long resolveFileSize(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        try {
            return Files.size(Path.of(filePath));
        } catch (Exception ignored) {
            return null;
        }
    }
}
