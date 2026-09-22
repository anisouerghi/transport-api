package com.transport.reporting.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.transport.reporting.config.QrProperties;
import com.transport.reporting.config.SharedStoragePaths;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;

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
    private static final int QR_SIZE = 360;
    private static final int LOGO_SIZE = 120;
    private static final int LOGO_PADDING = 6;
    private static final String LOGO_RESOURCE = "/qrcode/souteq_logo.png";

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
            BitMatrix matrix = writer.encode(publicUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                            EncodeHintType.MARGIN, 1
                    ));
                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
                BufferedImage colorQrImage = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D qrBaseGraphics = colorQrImage.createGraphics();
                qrBaseGraphics.drawImage(qrImage, 0, 0, null);
                qrBaseGraphics.dispose();
                addCenterLogo(colorQrImage);
                ImageIO.write(colorQrImage, "PNG", filePath.toFile());

            log.info("QR code generated for support {} at {}", support.getReference(), filePath);
            return filePath.toString();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate QR code: " + e.getMessage());
        }
    }

    private void addCenterLogo(BufferedImage qrImage) throws Exception {
        try (InputStream logoStream = QrCodeService.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (logoStream == null) {
                throw new IllegalStateException("QR logo resource not found: " + LOGO_RESOURCE);
            }

            BufferedImage logo = ImageIO.read(logoStream);
            int logoWidth = logo.getWidth();
            int logoHeight = logo.getHeight();
            double scale = Math.min((double) LOGO_SIZE / logoWidth, (double) LOGO_SIZE / logoHeight);
            int scaledWidth = Math.max(1, (int) Math.round(logoWidth * scale));
            int scaledHeight = Math.max(1, (int) Math.round(logoHeight * scale));

            BufferedImage resizedLogo = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D logoGraphics = resizedLogo.createGraphics();
            logoGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            logoGraphics.drawImage(logo.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH), 0, 0, null);
            logoGraphics.dispose();
            removeCheckerboardBackground(resizedLogo);

            int x = (qrImage.getWidth() - scaledWidth) / 2;
            int y = (qrImage.getHeight() - scaledHeight) / 2;
            Graphics2D qrGraphics = qrImage.createGraphics();
            qrGraphics.setColor(Color.WHITE);
            qrGraphics.fillRect(x - LOGO_PADDING, y - LOGO_PADDING,
                    scaledWidth + 2 * LOGO_PADDING, scaledHeight + 2 * LOGO_PADDING);
            qrGraphics.setComposite(AlphaComposite.SrcOver);
                qrGraphics.drawImage(resizedLogo, x, y, null);
            qrGraphics.dispose();
        }
    }

    private void removeCheckerboardBackground(BufferedImage logo) {
        int width = logo.getWidth();
        int height = logo.getHeight();
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<int[]> pending = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            pending.add(new int[]{x, 0});
            pending.add(new int[]{x, height - 1});
        }
        for (int y = 1; y < height - 1; y++) {
            pending.add(new int[]{0, y});
            pending.add(new int[]{width - 1, y});
        }

        while (!pending.isEmpty()) {
            int[] point = pending.removeFirst();
            int x = point[0];
            int y = point[1];
            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) {
                continue;
            }
            visited[y][x] = true;
            if (!isCheckerboardPixel(logo.getRGB(x, y))) {
                continue;
            }

            logo.setRGB(x, y, 0x00000000);
            pending.add(new int[]{x + 1, y});
            pending.add(new int[]{x - 1, y});
            pending.add(new int[]{x, y + 1});
            pending.add(new int[]{x, y - 1});
        }
    }

    private boolean isCheckerboardPixel(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int maximum = Math.max(red, Math.max(green, blue));
        int minimum = Math.min(red, Math.min(green, blue));
        return maximum - minimum <= 18 && minimum >= 135;
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
