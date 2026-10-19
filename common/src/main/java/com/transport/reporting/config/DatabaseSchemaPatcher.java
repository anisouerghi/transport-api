package com.transport.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Patches de schéma MySQL pour bases déjà initialisées (sans DROP complet).
 * Exécuté avant l'initialisation JPA pour compatibilité avec {@code ddl-auto=validate}.
 */
public final class DatabaseSchemaPatcher {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatcher.class);

    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

    private DatabaseSchemaPatcher() {
    }

    public static void apply(JdbcTemplate jdbcTemplate) {
        ensurePassengerPasswordHashColumn(jdbcTemplate);
        ensurePassengerGoogleOAuthColumns(jdbcTemplate);
        ensurePassengerEnrichmentColumns(jdbcTemplate);
        ensurePassengerPreferencesColumns(jdbcTemplate);
        ensurePassengerPreferenceTable(jdbcTemplate);
        ensurePassengerOtpChallengeTable(jdbcTemplate);
        ensureReplyPublicResponseColumn(jdbcTemplate);
        ensureReplyConversationColumns(jdbcTemplate);
        ensureReportSatisfactionTable(jdbcTemplate);
        ensureReportSupportNullable(jdbcTemplate);
        ensureI18nLabelColumns(jdbcTemplate);
        // report_type (INSERT + i18n/priority/icon) → ReportTypeDataInitializer (common)
        ensureReportTypePriority(jdbcTemplate);
        ensureReportTypeIcon(jdbcTemplate);
        ensureReportAnalysisIndexes(jdbcTemplate);
        ensureRoleReportTypeTable(jdbcTemplate);
    }

    /**
     * Association rôle ↔ types de signalement (filtrage métier des agents).
     * Remplace l'ancienne table role_report_nature si elle existe encore.
     */
    private static void ensureRoleReportTypeTable(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "role") || !tableExists(jdbcTemplate, "report_type")) {
            return;
        }
        try {
            if (tableExists(jdbcTemplate, "role_report_nature")) {
                jdbcTemplate.execute("DROP TABLE role_report_nature");
                log.info("Table role_report_nature retirée (remplacée par role_report_type).");
            }
            if (tableExists(jdbcTemplate, "role_report_type")) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE role_report_type (
                        role_id        BIGINT NOT NULL,
                        report_type_id BIGINT NOT NULL,
                        PRIMARY KEY (role_id, report_type_id),
                        CONSTRAINT fk_role_report_type_role
                            FOREIGN KEY (role_id) REFERENCES role (role_id),
                        CONSTRAINT fk_role_report_type_type
                            FOREIGN KEY (report_type_id) REFERENCES report_type (report_type_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            log.info("Table role_report_type créée.");
        } catch (Exception ex) {
            log.warn("Impossible d'assurer role_report_type : {}", ex.getMessage());
        }
    }

    /**
     * Ajoute {@code report_type.priority} et renseigne l'ordre initial
     * uniquement lorsque la valeur est encore vide.
     */
    private static void ensureReportTypePriority(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "report_type")) {
            return;
        }
        try {
            if (!columnExists(jdbcTemplate, "report_type", "priority")) {
                jdbcTemplate.execute("ALTER TABLE report_type ADD COLUMN priority INT NULL");
                log.info("Colonne report_type.priority ajoutée.");
            }
            jdbcTemplate.update("UPDATE report_type SET priority = 1 WHERE code = 'URGENCE' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 2 WHERE code = 'HARCELEMENT' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 3 WHERE code = 'COMPLAINT' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 4 WHERE code = 'INCIDENT' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 5 WHERE code = 'SUGGESTION' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 6 WHERE code = 'THANKS' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 7 WHERE code = 'OTHER' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 8 WHERE code = 'ASSAULT' AND priority IS NULL");
            jdbcTemplate.update("UPDATE report_type SET priority = 100 WHERE priority IS NULL");
            jdbcTemplate.execute("ALTER TABLE report_type MODIFY COLUMN priority INT NOT NULL DEFAULT 100");
        } catch (Exception ex) {
            log.warn("Impossible d'assurer report_type.priority : {}", ex.getMessage());
        }
    }

    /** Enregistre le nom Material Symbols déjà utilisé par le frontend. */
    private static void ensureReportTypeIcon(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "report_type")) {
            return;
        }
        try {
            if (!columnExists(jdbcTemplate, "report_type", "icon")) {
                jdbcTemplate.execute("ALTER TABLE report_type ADD COLUMN icon VARCHAR(80) NULL");
                log.info("Colonne report_type.icon ajoutée.");
            }
            setIconIfBlank(jdbcTemplate, "URGENCE", "crisis_alert");
            setIconIfBlank(jdbcTemplate, "HARCELEMENT", "crisis_alert");
            setIconIfBlank(jdbcTemplate, "COMPLAINT", "rate_review");
            setIconIfBlank(jdbcTemplate, "INCIDENT", "photo_camera");
            setIconIfBlank(jdbcTemplate, "SUGGESTION", "tips_and_updates");
            setIconIfBlank(jdbcTemplate, "THANKS", "thumb_up");
            setIconIfBlank(jdbcTemplate, "OTHER", "contact_support");
            setIconIfBlank(jdbcTemplate, "ASSAULT", "shield");
            jdbcTemplate.update(
                    "UPDATE report_type SET icon = 'shield' WHERE code = 'ASSAULT' AND icon = 'emergency'");
        } catch (Exception ex) {
            log.warn("Impossible d'assurer report_type.icon : {}", ex.getMessage());
        }
    }

    private static void setIconIfBlank(JdbcTemplate jdbcTemplate, String code, String icon) {
        jdbcTemplate.update(
                "UPDATE report_type SET icon = ? WHERE code = ? AND (icon IS NULL OR TRIM(icon) = '')",
                icon, code);
    }

    private static void ensurePassengerPasswordHashColumn(JdbcTemplate jdbcTemplate) {
        try {
            if (columnExists(jdbcTemplate, "passenger", "password_hash")) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE passenger ADD COLUMN password_hash VARCHAR(255) NULL");
            log.info("Colonne passenger.password_hash ajoutée (auth voyageur).");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter passenger.password_hash : {}", ex.getMessage());
        }
    }

    private static void ensurePassengerGoogleOAuthColumns(JdbcTemplate jdbcTemplate) {
        try {
            if (!columnExists(jdbcTemplate, "passenger", "google_subject")) {
                jdbcTemplate.execute("ALTER TABLE passenger ADD COLUMN google_subject VARCHAR(255) NULL");
                log.info("Colonne passenger.google_subject ajoutée (Google OAuth).");
            }
            if (!columnExists(jdbcTemplate, "passenger", "auth_provider")) {
                jdbcTemplate.execute(
                        "ALTER TABLE passenger ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'");
                log.info("Colonne passenger.auth_provider ajoutée (Google OAuth).");
            }
            if (!indexExists(jdbcTemplate, "passenger", "uk_passenger_google_subject")) {
                jdbcTemplate.execute(
                        "ALTER TABLE passenger ADD UNIQUE KEY uk_passenger_google_subject (google_subject)");
                log.info("Index uk_passenger_google_subject ajouté.");
            }
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter les colonnes Google OAuth passenger : {}", ex.getMessage());
        }
    }

    private static void ensurePassengerEnrichmentColumns(JdbcTemplate jdbcTemplate) {
        try {
            addColumnIfMissing(jdbcTemplate, "passenger", "profile_picture_url",
                    "ALTER TABLE passenger ADD COLUMN profile_picture_url VARCHAR(512) NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "last_ip",
                    "ALTER TABLE passenger ADD COLUMN last_ip VARCHAR(64) NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "last_user_agent",
                    "ALTER TABLE passenger ADD COLUMN last_user_agent VARCHAR(512) NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "last_browser",
                    "ALTER TABLE passenger ADD COLUMN last_browser VARCHAR(50) NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "latitude",
                    "ALTER TABLE passenger ADD COLUMN latitude DOUBLE NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "longitude",
                    "ALTER TABLE passenger ADD COLUMN longitude DOUBLE NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "gps_accuracy",
                    "ALTER TABLE passenger ADD COLUMN gps_accuracy DOUBLE NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "gps_captured_at",
                    "ALTER TABLE passenger ADD COLUMN gps_captured_at DATETIME(6) NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "last_auth_at",
                    "ALTER TABLE passenger ADD COLUMN last_auth_at DATETIME(6) NULL");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter les colonnes d'enrichissement passenger : {}",
                    ex.getMessage());
        }
    }

    /**
     * Préférences de profil voyageur : {@code notifications} (tableau JSON) et
     * {@code language} (code ISO 639-1 sur 2 lettres). Les deux sont optionnelles :
     * aucune valeur par défaut n'est injectée sur les lignes existantes.
     */
    private static void ensurePassengerPreferencesColumns(JdbcTemplate jdbcTemplate) {
        try {
            addColumnIfMissing(jdbcTemplate, "passenger", "notifications",
                    "ALTER TABLE passenger ADD COLUMN notifications JSON NULL");
            addColumnIfMissing(jdbcTemplate, "passenger", "language",
                    "ALTER TABLE passenger ADD COLUMN language VARCHAR(2) NULL");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter les préférences de profil passenger : {}",
                    ex.getMessage());
        }
    }

    /**
     * Table dédiée des préférences voyageur (1 ligne / passager).
     * Backfill depuis passenger.language / passenger.notifications si présents.
     */
    private static void ensurePassengerPreferenceTable(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "passenger")) {
            return;
        }
        try {
            if (!tableExists(jdbcTemplate, "passenger_preference")) {
                jdbcTemplate.execute("""
                        CREATE TABLE passenger_preference (
                            preference_id    BIGINT NOT NULL AUTO_INCREMENT,
                            passenger_id     BIGINT NOT NULL,
                            language         VARCHAR(2) NOT NULL DEFAULT 'FR',
                            notify_platform  TINYINT(1) NOT NULL DEFAULT 1,
                            notify_email     TINYINT(1) NOT NULL DEFAULT 1,
                            notify_sms       TINYINT(1) NOT NULL DEFAULT 0,
                            notify_whatsapp  TINYINT(1) NOT NULL DEFAULT 0,
                            created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),
                            PRIMARY KEY (preference_id),
                            UNIQUE KEY uk_passenger_preference_passenger (passenger_id),
                            CONSTRAINT fk_passenger_preference_passenger
                                FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """);
                log.info("Table passenger_preference créée.");
            }
            // Backfill : comptes ayant déjà language/notifications sur passenger.
            jdbcTemplate.execute("""
                    INSERT INTO passenger_preference (
                        passenger_id, language, notify_platform, notify_email, notify_sms, notify_whatsapp,
                        created_at, updated_at)
                    SELECT
                        p.passenger_id,
                        CASE
                            WHEN UPPER(TRIM(p.language)) IN ('FR', 'AR', 'EN') THEN UPPER(TRIM(p.language))
                            ELSE 'FR'
                        END,
                        CASE WHEN p.notifications IS NOT NULL
                              AND JSON_CONTAINS(p.notifications, '1', '$') THEN 1 ELSE
                             CASE WHEN p.notifications IS NULL THEN 1 ELSE 0 END END,
                        CASE WHEN p.notifications IS NOT NULL
                              AND JSON_CONTAINS(p.notifications, '2', '$') THEN 1 ELSE
                             CASE WHEN p.notifications IS NULL THEN 1 ELSE 0 END END,
                        CASE WHEN p.notifications IS NOT NULL
                              AND JSON_CONTAINS(p.notifications, '3', '$') THEN 1 ELSE 0 END,
                        CASE WHEN p.notifications IS NOT NULL
                              AND JSON_CONTAINS(p.notifications, '4', '$') THEN 1 ELSE 0 END,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    FROM passenger p
                    WHERE (p.password_hash IS NOT NULL AND p.password_hash <> '')
                       OR (p.google_subject IS NOT NULL AND p.google_subject <> '')
                    ON DUPLICATE KEY UPDATE passenger_id = passenger_id
                    """);
        } catch (Exception ex) {
            log.warn("Impossible d'assurer passenger_preference : {}", ex.getMessage());
        }
    }

    private static void addColumnIfMissing(
            JdbcTemplate jdbcTemplate, String table, String column, String alterSql) {
        if (columnExists(jdbcTemplate, table, column)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
        log.info("Colonne {}.{} ajoutée.", table, column);
    }

    private static void ensurePassengerOtpChallengeTable(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'passenger_otp_challenge'",
                    Integer.class);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE passenger_otp_challenge (
                        challenge_id   BIGINT NOT NULL AUTO_INCREMENT,
                        transaction_id VARCHAR(36)  NOT NULL,
                        passenger_id   BIGINT       NOT NULL,
                        otp_hash       VARCHAR(255) NOT NULL,
                        attempt_count  INT          NOT NULL DEFAULT 0,
                        send_count     INT          NOT NULL DEFAULT 1,
                        expires_at     DATETIME(6)  NOT NULL,
                        last_sent_at   DATETIME(6)  NOT NULL,
                        status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                        created_at     DATETIME(6)  NOT NULL,
                        PRIMARY KEY (challenge_id),
                        UNIQUE KEY uk_otp_transaction (transaction_id),
                        KEY idx_otp_challenge_passenger (passenger_id),
                        KEY idx_otp_challenge_status (status)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            log.info("Table passenger_otp_challenge créée (OTP e-mail voyageur).");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/créer passenger_otp_challenge : {}", ex.getMessage());
        }
    }

    private static void ensureReplyPublicResponseColumn(JdbcTemplate jdbcTemplate) {
        try {
            if (columnExists(jdbcTemplate, "reply", "public_response")) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE reply ADD COLUMN public_response TINYINT(1) NOT NULL DEFAULT 1");
            log.info("Colonne reply.public_response ajoutée.");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter reply.public_response : {}", ex.getMessage());
        }
    }

    /**
     * Colonnes de conversation sur {@code reply}, sans suppression ni renommage.
     * Les messages déjà présents deviennent une réponse agent.
     */
    private static void ensureReplyConversationColumns(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "reply")) {
            return;
        }
        try {
            if (!columnExists(jdbcTemplate, "reply", "reply_type")) {
                jdbcTemplate.execute("ALTER TABLE reply ADD COLUMN reply_type VARCHAR(40) NULL");
            }
            jdbcTemplate.update(
                    "UPDATE reply SET reply_type = 'RESPONSE' WHERE reply_type IS NULL OR TRIM(reply_type) = ''");
            jdbcTemplate.execute(
                    "ALTER TABLE reply MODIFY COLUMN reply_type VARCHAR(40) NOT NULL DEFAULT 'RESPONSE'");

            if (!columnExists(jdbcTemplate, "reply", "author_type")) {
                jdbcTemplate.execute("ALTER TABLE reply ADD COLUMN author_type VARCHAR(20) NULL");
            }
            jdbcTemplate.update(
                    "UPDATE reply SET author_type = 'AGENT' WHERE author_type IS NULL OR TRIM(author_type) = ''");
            jdbcTemplate.execute(
                    "ALTER TABLE reply MODIFY COLUMN author_type VARCHAR(20) NOT NULL DEFAULT 'AGENT'");

            if (!columnExists(jdbcTemplate, "reply", "passenger_id")) {
                jdbcTemplate.execute("ALTER TABLE reply ADD COLUMN passenger_id BIGINT NULL");
            }
            if (!foreignKeyExists(jdbcTemplate, "reply", "fk_reply_passenger")) {
                jdbcTemplate.execute(
                        "ALTER TABLE reply ADD CONSTRAINT fk_reply_passenger "
                                + "FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id)");
            }
            log.info("Colonnes de conversation reply assurées.");
        } catch (Exception ex) {
            log.warn("Impossible d'assurer les colonnes de conversation reply : {}", ex.getMessage());
        }
    }

    private static boolean foreignKeyExists(JdbcTemplate jdbcTemplate, String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                        + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                        + "AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                Integer.class,
                tableName,
                constraintName);
        return count != null && count > 0;
    }

    /**
     * Avis unique du voyageur. Les signalements déjà présents n'ont pas d'avis.
     */
    private static void ensureReportSatisfactionTable(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "report") || tableExists(jdbcTemplate, "report_satisfaction")) {
            return;
        }
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE report_satisfaction (
                        satisfaction_id    BIGINT       NOT NULL AUTO_INCREMENT,
                        report_id          BIGINT       NOT NULL,
                        satisfaction_level VARCHAR(40)  NOT NULL,
                        comment            TEXT         NULL,
                        created_at         DATETIME(6)  NOT NULL,
                        PRIMARY KEY (satisfaction_id),
                        UNIQUE KEY uk_report_satisfaction_report (report_id),
                        CONSTRAINT fk_report_satisfaction_report
                            FOREIGN KEY (report_id) REFERENCES report (report_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            log.info("Table report_satisfaction créée.");
        } catch (Exception ex) {
            log.warn("Impossible de créer report_satisfaction : {}", ex.getMessage());
        }
    }

    private static void ensureReportSupportNullable(JdbcTemplate jdbcTemplate) {
        try {
            if (!columnExists(jdbcTemplate, "report", "transport_support_id")) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE report MODIFY COLUMN transport_support_id BIGINT NULL");
            log.info("Colonne report.transport_support_id rendue optionnelle.");
        } catch (Exception ex) {
            log.warn("Impossible de rendre report.transport_support_id optionnelle : {}", ex.getMessage());
        }
    }

    /**
     * Colonnes i18n V1 (label_fr / label_ar / label_en) — complementary au script SQL doc.
     * Copie {@code label} → {@code label_fr} si vide. Les traductions AR/EN restent
     * à appliquer via {@code documentation/migration-i18n-param-labels.sql} ou seeders.
     */
    private static void ensureI18nLabelColumns(JdbcTemplate jdbcTemplate) {
        try {
            ensureCatalogI18n(jdbcTemplate, "support_type", 150);
            ensureCatalogI18n(jdbcTemplate, "report_type", 150);
            ensureCatalogI18n(jdbcTemplate, "report_nature", 150);
            ensureCatalogI18n(jdbcTemplate, "report_status", 100);
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter les colonnes i18n label_* : {}", ex.getMessage());
        }
    }

    private static void ensureCatalogI18n(JdbcTemplate jdbcTemplate, String table, int maxLen) {
        if (!tableExists(jdbcTemplate, table)) {
            return;
        }
        addColumnIfMissing(jdbcTemplate, table, "label_fr",
                "ALTER TABLE " + table + " ADD COLUMN label_fr VARCHAR(" + maxLen + ") NULL");
        addColumnIfMissing(jdbcTemplate, table, "label_ar",
                "ALTER TABLE " + table + " ADD COLUMN label_ar VARCHAR(" + maxLen + ") NULL");
        addColumnIfMissing(jdbcTemplate, table, "label_en",
                "ALTER TABLE " + table + " ADD COLUMN label_en VARCHAR(" + maxLen + ") NULL");
        jdbcTemplate.execute(
                "UPDATE " + table + " SET label_fr = label WHERE label_fr IS NULL OR TRIM(label_fr) = ''");
    }

    /** Accélère les filtres de période, de type et de support de l'analyse. */
    private static void ensureReportAnalysisIndexes(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "report")) {
            return;
        }
        try {
            if (!indexExists(jdbcTemplate, "report", "idx_report_creation_date")) {
                jdbcTemplate.execute("CREATE INDEX idx_report_creation_date ON report (creation_date)");
            }
            if (!indexExists(jdbcTemplate, "report", "idx_report_analysis")) {
                jdbcTemplate.execute(
                        "CREATE INDEX idx_report_analysis ON report (creation_date, report_type_id, transport_support_id)");
            }
        } catch (Exception ex) {
            log.warn("Impossible de créer les index d'analyse des signalements : {}", ex.getMessage());
        }
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private static boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                COLUMN_EXISTS_SQL,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private static boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName);
        return count != null && count > 0;
    }
}
