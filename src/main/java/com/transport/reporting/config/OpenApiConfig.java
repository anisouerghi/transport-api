package com.transport.reporting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI transportOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transport Reporting API")
                        .description("""
                                API de signalement transport public.

                                - `/api/public` : application Angular Voyageur
                                - `/api/admin` : application Angular Administration (Basic Auth)
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Équipe Transport").email("support@transport.local")))
                .servers(List.of(new Server().url("/").description("Serveur courant")))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
