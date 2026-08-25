package com.transport.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée Admin API — port par défaut 8082.
 * Propriétaire du schéma / seed en environnement de développement.
 */
@SpringBootApplication
public class AdminApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
