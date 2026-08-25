package com.transport.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Coquille transitoire du monolithe (plus de routes métier).
 * Utiliser {@code public-api} (8081) et {@code admin-api} (8082).
 */
@SpringBootApplication
public class TransportApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportApplication.class, args);
    }
}
