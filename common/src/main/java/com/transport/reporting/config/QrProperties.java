package com.transport.reporting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Proprietes de configuration des QR codes (app.qr.*).
 */
@Component
@ConfigurationProperties(prefix = "app.qr")
@Getter
@Setter
public class QrProperties {

    /**
     * URL de base utilisee pour construire les liens publics.
     * Format final (Hash Routing) : {baseUrl}/#/report/{uuid}
     * Ex. {@code http://192.168.1.55/sig/} → {@code http://192.168.1.55/sig/#/report/{uuid}}
     */
    private String baseUrl;

    /**
     * Repertoire de stockage des images QR Code generees.
     */
    private String storagePath = "data/qr-codes";
}
