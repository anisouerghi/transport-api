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

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensurePassengerPasswordHashColumn();
    }

    private void ensurePassengerPasswordHashColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'passenger'
                      AND COLUMN_NAME = 'password_hash'
                    """,
                    Integer.class);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE passenger ADD COLUMN password_hash VARCHAR(255) NULL");
            log.info("Colonne passenger.password_hash ajoutée (auth voyageur).");
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/ajouter passenger.password_hash : {}", ex.getMessage());
        }
    }
}
