package com.transport.reporting.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.transport.reporting.config.QrProperties;
import com.transport.reporting.config.SharedStoragePaths;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service de generation et lecture des images QR Code (bibliotheque ZXing).
 * <p>
 * Configuration :
 * <ul>
 *   <li>{@code app.qr.base-url} — ex. http://localhost:4200 ou http://192.168.1.55/sig/</li>
 *   <li>{@code app.qr.storage-path} — repertoire de stockage des PNG</li>
 * </ul>
 * URL encodee dans le QR : {@code {baseUrl}/report/{uuid}}
 */
@Service
@Slf4j
public class QrCodeService {

    /** Taille de l'image QR en pixels (carre). */
    private static final int QR_SIZE = 300;

    private final QrProperties qrProperties;
    private final SharedStoragePaths sharedStoragePaths;

    public QrCodeService(QrProperties qrProperties, SharedStoragePaths sharedStoragePaths) {
        this.qrProperties = qrProperties;
        this.sharedStoragePaths = sharedStoragePaths;
    }


    /**
     * Construit l'URL publique de signalement pour un support.
     *
     * @return ex. http://192.168.1.55/sig/report/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    public String buildPublicUrl(TransportSupport support) {
        String baseUrl = qrProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException("QR base URL is not configured (app.qr.base-url)");
        }
        // Evite les doubles slash si baseUrl se termine par /
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/report/" + support.getUuid();
    }

    /**
     * Genere l'image QR (PNG), l'enregistre sur disque et retourne le chemin absolu.
     * Le contenu encode dans le QR est l'URL publique de signalement.
     */
    public String generateAndStore(TransportSupport support) {
        try {
            String publicUrl = buildPublicUrl(support);
            Path storageDir = sharedStoragePaths.qrRoot();
            Files.createDirectories(storageDir);

            String fileName = support.getUuid() + ".png";
            Path filePath = storageDir.resolve(fileName);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(publicUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToPath(matrix, "PNG", filePath);

            log.info("QR code generated for support {} at {}", support.getReference(), filePath);
            return filePath.toString();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate QR code: " + e.getMessage());
        }
    }

    /**
     * Lit le fichier PNG du QR Code depuis le disque.
     *
     * @return octets de l'image PNG
     */
    public byte[] readQrImage(TransportSupport support) {
        if (support.getQrCodePath() == null || support.getQrCodePath().isBlank()) {
            throw new BusinessException("QR code file not found for this support");
        }
        try {
            Path path = Path.of(support.getQrCodePath()).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                // Repli : même UUID sous le répertoire QR partagé (après migration de chemin)
                Path shared = sharedStoragePaths.qrRoot().resolve(path.getFileName()).normalize();
                if (!shared.startsWith(sharedStoragePaths.qrRoot()) || !Files.exists(shared)) {
                    throw new BusinessException("QR code file not found on disk");
                }
                path = shared;
            }
            return Files.readAllBytes(path);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to read QR code file: " + e.getMessage());
        }
    }
}
