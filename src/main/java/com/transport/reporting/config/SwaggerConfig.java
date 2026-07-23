package com.transport.reporting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI transportOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transport Reporting API")
                        .description("""
                                API de signalement transport public.

                                - `/api/public/**` : Angular Voyageur
                                - `/api/admin/**` : Angular Administration
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Équipe Transport").email("support@transport.local")))
                .servers(List.of(new Server().url("/").description("Serveur local")));
    }
}
