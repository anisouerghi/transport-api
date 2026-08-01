package com.transport.reporting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration Swagger / OpenAPI (schéma Bearer JWT).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI transportOpenAPI() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Transport Reporting API")
                        .description("API de signalement transport public. "
                                + "/api/public/** : Angular Voyageur ; "
                                + "/api/admin/** : Angular Administration (JWT) ; "
                                + "/api/auth/login : authentification")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("/").description("Serveur local")))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
