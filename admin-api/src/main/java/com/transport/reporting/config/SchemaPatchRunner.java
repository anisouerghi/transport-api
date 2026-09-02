package com.transport.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Patches de schéma (secours au démarrage admin-api).
 * Le patch principal s'exécute avant JPA via {@link EarlyDatabaseSchemaPatchConfiguration}.
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
        log.debug("Vérification des patches de schéma (secours).");
        DatabaseSchemaPatcher.apply(jdbcTemplate);
    }
}
