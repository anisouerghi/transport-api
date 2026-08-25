package com.transport.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée Public API (voyageur) — port par défaut 8081.
 */
@SpringBootApplication
public class PublicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicApiApplication.class, args);
    }
}
