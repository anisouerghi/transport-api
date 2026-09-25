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
        ensurePassengerOtpChallengeTable(jdbcTemplate);
        ensureReplyPublicResponseColumn(jdbcTemplate);
        ensureReportSupportNullable(jdbcTemplate);
        ensureI18nLabelColumns(jdbcTemplate);
        ensureSixPublicReportTypes(jdbcTemplate);
    }

    /**
     * Garantit les 6 natures voyageur dans {@code report_type} (idempotent).
     * Nécessaire car public-api peut démarrer sans passer par les seeders admin-only.
     */
    private static void ensureSixPublicReportTypes(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "report_type")) {
            return;
        }
        try {
            insertReportTypeIfMissing(jdbcTemplate, "COMPLAINT", "Réclamation", "شكوى", "Complaint",
                    "Réclamation voyageur");
            insertReportTypeIfMissing(jdbcTemplate, "ASSAULT", "Agression", "اعتداء", "Assault",
                    "Signalement d'agression ou de violence");
            insertReportTypeIfMissing(jdbcTemplate, "INCIDENT", "Incident", "حادث", "Incident",
                    "Incident technique ou sécurité");
            insertReportTypeIfMissing(jdbcTemplate, "SUGGESTION", "Suggestion", "اقتراح", "Suggestion",
                    "Suggestion d'amélioration");
            insertReportTypeIfMissing(jdbcTemplate, "THANKS", "Remerciement", "شكر", "Thank you",
                    "Remerciement");
            insertReportTypeIfMissing(jdbcTemplate, "OTHER", "Autre", "أخرى", "Other",
                    "Autre nature de signalement");
        } catch (Exception ex) {
            log.warn("Impossible d'assurer les 6 report_type publics : {}", ex.getMessage());
        }
    }

    private static void insertReportTypeIfMissing(
            JdbcTemplate jdbcTemplate,
            String code,
            String labelFr,
            String labelAr,
            String labelEn,
            String description) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_type WHERE code = ?",
                Integer.class,
                code);
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "UPDATE report_type SET active = 1 WHERE code = ? AND active = 0",
                    code);
            return;
        }
        boolean hasI18n = columnExists(jdbcTemplate, "report_type", "label_fr");
        if (hasI18n) {
            jdbcTemplate.update(
                    "INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    code, labelFr, labelFr, labelAr, labelEn, description);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO report_type (code, label, description, active) VALUES (?, ?, ?, 1)",
                    code, labelFr, description);
        }
        log.info("report_type {} créé (nature voyageur).", code);
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
