package com.transport.reporting.service;

import com.transport.reporting.dto.AttachmentResponse;
import com.transport.reporting.entity.Attachment;
import com.transport.reporting.entity.Report;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.AttachmentMapper;
import com.transport.reporting.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service métier des pièces jointes rattachées aux signalements.
 * <p>
 * Orchestre la validation/stockage fichier ({@link FileStorageService})
 * et la persistence JPA ({@link Attachment}).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;

    /**
     * Enregistre les fichiers uploadés pour un signalement déjà persisté.
     *
     * @param report signalement propriétaire (obligatoire si des fichiers sont fournis)
     * @param files  fichiers optionnels ; null ou vide = aucune pièce jointe
     * @return liste des pièces jointes créées
     */
    public List<AttachmentResponse> saveForReport(Report report, MultipartFile[] files) {
        fileStorageService.validateBatch(files);
        if (files == null || files.length == 0) {
            return List.of();
        }

        List<AttachmentResponse> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            FileStorageService.StoredFile stored = fileStorageService.store(file);
            Attachment attachment = Attachment.builder()
                    .fileName(stored.originalFileName())
                    .filePath(stored.absolutePath())
                    .fileType(stored.mimeType())
                    .report(report)
                    .build();
            saved.add(attachmentMapper.toResponse(attachmentRepository.save(attachment)));
        }
        return saved;
    }

    /**
     * Liste les pièces jointes d'un signalement (ordre repository / insertion).
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> findByReportId(Long reportId) {
        return attachmentRepository.findByReport_ReportId(reportId).stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    /**
     * Charge l'entité pièce jointe ou lève {@link ResourceNotFoundException}.
     */
    @Transactional(readOnly = true)
    public Attachment getEntity(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
    }

    /**
     * Lit le contenu binaire d'une pièce jointe pour affichage ou téléchargement.
     */
    @Transactional(readOnly = true)
    public byte[] readContent(Long attachmentId) {
        Attachment attachment = getEntity(attachmentId);
        return fileStorageService.readBytes(attachment.getFilePath());
    }
}
