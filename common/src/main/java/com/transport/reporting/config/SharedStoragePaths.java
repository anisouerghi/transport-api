package com.transport.reporting.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Résout les chemins PJ / QR en chemins <strong>absolus partagés</strong> pour public-api et admin-api.
 * <p>
 * Priorité :
 * <ol>
 *   <li>{@code APP_UPLOAD_PATH} / {@code APP_QR_STORAGE_PATH} (ou propriétés équivalentes) si absolus ;</li>
 *   <li>sinon chemins relatifs résolus sous {@code APP_STORAGE_ROOT} s'il est défini ;</li>
 *   <li>sinon sous la racine multi-module (dossier contenant {@code public-api} + {@code admin-api}).</li>
 * </ol>
 * Évite deux arbres {@code data/} différents selon le working directory Maven.
 */
@Component
public class SharedStoragePaths {

    private static final Logger log = LoggerFactory.getLogger(SharedStoragePaths.class);

    private final Path uploadRoot;
    private final Path qrRoot;

    public SharedStoragePaths(
            @Value("${app.upload.path}") String uploadPath,
            @Value("${app.qr.storage-path}") String qrStoragePath,
            @Value("${app.storage.root:}") String storageRoot) {
        Path root = resolveStorageRoot(storageRoot);
        this.uploadRoot = resolveConfiguredPath(uploadPath, root, "data/attachments");
        this.qrRoot = resolveConfiguredPath(qrStoragePath, root, "data/qr-codes");
    }

    @PostConstruct
    void logResolvedPaths() {
        log.info("Shared storage root resolved — upload={} | qr={}", uploadRoot, qrRoot);
    }

    public Path uploadRoot() {
        return uploadRoot;
    }

    public Path qrRoot() {
        return qrRoot;
    }

    private static Path resolveStorageRoot(String configuredRoot) {
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot).toAbsolutePath().normalize();
        }
        return findMultiModuleRoot();
    }

    private static Path resolveConfiguredPath(String configured, Path storageRoot, String defaultRelative) {
        String value = (configured == null || configured.isBlank()) ? defaultRelative : configured.trim();
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        // "./data/..." or "../data/..." → normaliser puis rattacher à la racine repo (pas au CWD)
        Path normalized = path.normalize();
        String asString = normalized.toString().replace('\\', '/');
        while (asString.startsWith("../")) {
            asString = asString.substring(3);
        }
        if (asString.startsWith("./")) {
            asString = asString.substring(2);
        }
        if (asString.isBlank()) {
            asString = defaultRelative;
        }
        return storageRoot.resolve(asString).toAbsolutePath().normalize();
    }

    /**
     * Remonte depuis {@code user.dir} jusqu'au parent multi-module (public-api + admin-api présents).
     */
    static Path findMultiModuleRoot() {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path fallback = dir;
        for (int i = 0; i < 10 && dir != null; i++) {
            boolean hasPublic = Files.isDirectory(dir.resolve("public-api"));
            boolean hasAdmin = Files.isDirectory(dir.resolve("admin-api"));
            boolean hasCommon = Files.isDirectory(dir.resolve("common"));
            if (hasPublic && hasAdmin && hasCommon) {
                return dir;
            }
            dir = dir.getParent();
        }
        log.warn("Multi-module root not found from user.dir={} — using {}", fallback, fallback);
        return Objects.requireNonNullElse(fallback, Path.of(".").toAbsolutePath().normalize());
    }
}
