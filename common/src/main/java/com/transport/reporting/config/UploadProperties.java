package com.transport.reporting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriétés de configuration du stockage des pièces jointes ({@code app.upload.*}).
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String path = "./data/attachments";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
