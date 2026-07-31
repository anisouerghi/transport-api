package com.transport.reporting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriétés de configuration du stockage des pièces jointes ({@code app.upload.*}).
 * <p>
 * Exemple (profil dev) : {@code app.upload.path=./data/attachments}
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadProperties {

    /**
     * Répertoire racine de stockage des fichiers uploadés.
     * Ne doit jamais être codé en dur dans les services.
     */
    private String path = "./data/attachments";
}
