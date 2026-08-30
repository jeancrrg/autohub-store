package com.autohubstore.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                                .url("http://localhost:8002")
                                .description("Desenvolvimento local")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Auth Service API")
                .description("""
                        Microsserviço responsável por autenticação do AutoHubStore: login, logout,
                        refresh (rotation) e reset de senha. Extraído do User Service — não tem
                        tabela de usuário própria; valida e atualiza credencial chamando o User
                        Service via OpenFeign (`/internal/v1/users/**`), nunca acessando o
                        `password_hash` diretamente.

                        **Responsabilidades:**
                        - Login, logout, refresh e reset de senha (JWT + refresh token + blacklist Redis)
                        - Publicação do evento `user.password-reset` no Kafka

                        **Autenticação:** todos os endpoints `/api/v1/auth/*` são públicos. O JWT
                        emitido aqui é o mesmo validado pelo Gateway e pelos demais microsserviços.
                        """)
                .version("1.0.0");
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT emitido por este serviço. Em produção é entregue como cookie httpOnly "
                        + "access_token; o esquema Bearer aqui existe apenas para testar via Swagger UI.");
    }

}
