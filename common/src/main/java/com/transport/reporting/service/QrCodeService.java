package com.transport.reporting.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.transport.reporting.config.QrProperties;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service de generation et lecture des images QR Code (bibliotheque ZXing).
 * <p>
 * Configuration :
 * <ul>
 *   <li>{@code app.qr.base-url} — ex. http://localhost:4200</li>
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
    public QrCodeService(QrProperties qrProperties) {
        this.qrProperties = qrProperties;
    }


    /**
     * Construit l'URL publique de signalement pour un support.
     *
     * @return ex. http://localhost:4200/report/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
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
            Path storageDir = Paths.get(qrProperties.getStoragePath()).toAbsolutePath().normalize();
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
            Path path = Paths.get(support.getQrCodePath());
            if (!Files.exists(path)) {
                throw new BusinessException("QR code file not found on disk");
            }
            return Files.readAllBytes(path);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to read QR code file: " + e.getMessage());
        }
    }
}
