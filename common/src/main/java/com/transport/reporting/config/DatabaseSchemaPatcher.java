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
        ensurePassengerOtpChallengeTable(jdbcTemplate);
        ensureReplyPublicResponseColumn(jdbcTemplate);
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
