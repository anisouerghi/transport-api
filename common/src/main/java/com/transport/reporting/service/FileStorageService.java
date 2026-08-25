package com.transport.reporting.service;

import com.transport.reporting.config.SharedStoragePaths;
import com.transport.reporting.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage sécurisé des pièces jointes sur le système de fichiers.
 * <p>
 * Responsabilités :
 * <ul>
 *   <li>valider extension, type MIME et tailles (par fichier et cumulées) ;</li>
 *   <li>enregistrer chaque fichier sous un nom physique unique (UUID) ;</li>
 *   <li>lire le contenu en s'assurant que le chemin reste sous le répertoire partagé
 *       ({@link com.transport.reporting.config.SharedStoragePaths#uploadRoot()}).</li>
 * </ul>
 * Le nom d'origine fourni par le client n'est jamais utilisé comme nom de stockage.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Nombre maximum de pièces jointes par signalement. */
    public static final int MAX_FILES = 5;

    /** Taille maximale d'un fichier (10 Mo). */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** Taille maximale cumulée de toutes les pièces jointes (25 Mo). */
    public static final long MAX_TOTAL_BYTES = 25L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_EXTENSIONS_TO_MIME = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "webp", "image/webp",
            "pdf", MediaType.APPLICATION_PDF_VALUE
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            MediaType.APPLICATION_PDF_VALUE
    );

    private final SharedStoragePaths sharedStoragePaths;

    public FileStorageService(SharedStoragePaths sharedStoragePaths) {
        this.sharedStoragePaths = sharedStoragePaths;
    }

    /**
     * Valide puis enregistre un fichier sur disque.
     *
     * @param file fichier multipart non vide
     * @return métadonnées du fichier stocké (nom d'origine, chemin absolu, MIME, taille)
     * @throws BusinessException si le format, la taille ou le chemin est invalide
     */
    public StoredFile store(MultipartFile file) {
        validateSingle(file);

        String originalName = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String mimeType = resolveMimeType(file, extension);
        String expectedMime = ALLOWED_EXTENSIONS_TO_MIME.get(extension);
        if (expectedMime == null || !ALLOWED_MIME_TYPES.contains(mimeType) || !expectedMime.equals(mimeType)) {
            throw new BusinessException(
                    "Format de fichier non autorisé. Formats acceptés : JPG, JPEG, PNG, WEBP, PDF.");
        }

        try {
            Path storageDir = sharedStoragePaths.uploadRoot();
            Files.createDirectories(storageDir);

            String storedName = UUID.randomUUID() + "." + extension;
            Path target = storageDir.resolve(storedName).normalize();
            if (!target.startsWith(storageDir)) {
                throw new BusinessException("Chemin de stockage invalide.");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Attachment stored as {} (original={})", storedName, originalName);
            return new StoredFile(originalName, target.toString(), mimeType, file.getSize());
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("Échec de l'enregistrement du fichier : " + e.getMessage());
        }
    }

    /**
     * Lit les octets d'un fichier déjà stocké, avec contrôle anti path-traversal.
     *
     * @param filePath chemin absolu enregistré en base
     * @return contenu binaire
     */
    public byte[] readBytes(String filePath) {
        try {
            Path path = Path.of(filePath).toAbsolutePath().normalize();
            Path storageDir = sharedStoragePaths.uploadRoot();
            if (!path.startsWith(storageDir) || !Files.exists(path) || !Files.isRegularFile(path)) {
                throw new BusinessException("Fichier introuvable sur le serveur.");
            }
            return Files.readAllBytes(path);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier.");
        }
    }

    /**
     * Valide un lot de fichiers avant toute persistence métier
     * (nombre, taille unitaire, taille totale, extension).
     *
     * @param files tableau optionnel (null ou vide = aucune contrainte)
     */
    public void validateBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return;
        }
        MultipartFile[] nonEmpty = java.util.Arrays.stream(files)
                .filter(f -> f != null && !f.isEmpty())
                .toArray(MultipartFile[]::new);
        if (nonEmpty.length > MAX_FILES) {
            throw new BusinessException("Maximum " + MAX_FILES + " pièces jointes autorisées par signalement.");
        }
        long total = 0;
        for (MultipartFile file : nonEmpty) {
            validateSingle(file);
            total += file.getSize();
        }
        if (total > MAX_TOTAL_BYTES) {
            throw new BusinessException(
                    "La taille totale des pièces jointes ne doit pas dépasser 25 Mo.");
        }
    }

    /** Contrôle unitaire : présence, taille max et extension autorisée. */
    private void validateSingle(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Un fichier vide a été détecté.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(
                    "Chaque fichier ne doit pas dépasser 10 Mo (" + sanitizeOriginalFilename(file.getOriginalFilename()) + ").");
        }
        String originalName = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS_TO_MIME.containsKey(extension)) {
            throw new BusinessException(
                    "Format de fichier non autorisé. Formats acceptés : JPG, JPEG, PNG, WEBP, PDF.");
        }
    }

    /**
     * Détermine le MIME à partir du Content-Type client, avec repli sur l'extension.
     * Normalise {@code image/jpg} vers {@code image/jpeg}.
     */
    private String resolveMimeType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            String mime = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if ("image/jpg".equals(mime)) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            return mime;
        }
        return ALLOWED_EXTENSIONS_TO_MIME.get(extension);
    }

    /** Retire tout chemin relatif / traversal du nom fourni par le navigateur. */
    private String sanitizeOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("Nom de fichier manquant.");
        }
        String cleaned = Path.of(originalFilename).getFileName().toString().trim();
        if (cleaned.isBlank() || cleaned.contains("..")) {
            throw new BusinessException("Nom de fichier invalide.");
        }
        return cleaned;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new BusinessException("Extension de fichier manquante.");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Résultat du stockage d'un fichier sur disque.
     */
    public static final class StoredFile {
        private final String originalFileName;
        private final String absolutePath;
        private final String mimeType;
        private final long size;

        public StoredFile(String originalFileName, String absolutePath, String mimeType, long size) {
            this.originalFileName = originalFileName;
            this.absolutePath = absolutePath;
            this.mimeType = mimeType;
            this.size = size;
        }

        public String originalFileName() {
            return originalFileName;
        }

        public String absolutePath() {
            return absolutePath;
        }

        public String mimeType() {
            return mimeType;
        }

        public long size() {
            return size;
        }
    }
}
