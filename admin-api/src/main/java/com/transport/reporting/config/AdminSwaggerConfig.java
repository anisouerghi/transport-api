package com.transport.reporting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Admin API — schéma Bearer JWT obligatoire pour /api/admin.
 */
@Configuration
public class AdminSwaggerConfig {

    @Bean
    public OpenAPI adminOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("TRANSTU Admin API")
                        .description("API administration — /api/auth/** + /api/admin/**")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
