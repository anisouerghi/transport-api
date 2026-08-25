package com.transport.reporting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicSwaggerConfig {

    @Bean
    public OpenAPI publicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TRANSTU Public API")
                        .description("API voyageur — signalements, supports, auth passenger")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
