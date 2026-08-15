package com.transport.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Patches de schéma pour bases déjà initialisées (sans DROP complet).
 */
@Component
@Order(0)
public class SchemaPatchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatchRunner.class);

    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensurePassengerPasswordHashColumn();
        ensureReplyPublicResponseColumn();
    }

    private void ensurePassengerPasswordHashColumn() {
        try {
            if (columnExists("passenger", "password_hash")) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE passenger ADD COLUMN password_hash VARCHAR(255) NULL");
            log.info("Colonne passenger.password_hash ajoutée (auth voyageur).");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter passenger.password_hash : {}", ex.getMessage());
        }
    }

    private void ensureReplyPublicResponseColumn() {
        try {
            if (columnExists("reply", "public_response")) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE reply ADD COLUMN public_response TINYINT(1) NOT NULL DEFAULT 1");
            log.info("Colonne reply.public_response ajoutée.");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter reply.public_response : {}", ex.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                COLUMN_EXISTS_SQL,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
