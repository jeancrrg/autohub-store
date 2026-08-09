package com.autohubstore.catalogservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8003")
                                .description("Desenvolvimento local"),
                        new Server()
                                .url("http://catalog-service:8003")
                                .description("Docker Compose (rede interna)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Catalog Service API")
                .description("""
                        Microsserviço responsável pelo catálogo de produtos e categorias do AutoHubStore.

                        **Responsabilidades:**
                        - CRUD completo de produtos e categorias (admin)
                        - Listagem paginada de produtos com filtro por categoria
                        - Cache de produtos no Redis (TTL 5 minutos)
                        - Publicação dos eventos `catalog.product-created`, `catalog.product-updated`
                          e `catalog.product-viewed` no Kafka

                        **Autenticação:** endpoints de leitura são públicos. Endpoints de escrita (admin)
                        requerem Bearer JWT com role ADMIN, validado pelo API Gateway.
                        """)
                .version("0.0.1")
                .contact(new Contact()
                        .name("AutoHubStore")
                        .email("dev@autohubstore.com")
                        .url("https://github.com/autohubstore"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token emitido pelo Auth Service. Formato: `Bearer <token>`");
    }

}
