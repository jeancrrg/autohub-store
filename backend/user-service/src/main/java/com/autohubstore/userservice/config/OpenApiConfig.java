package com.autohubstore.userservice.config;

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
                                .url("http://user-service:8003")
                                .description("Docker Compose (rede interna)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("User Service API")
                .description("""
                        Microsserviço responsável por cadastro, perfil e endereços de usuários do
                        AutoHubStore. Não lida com autenticação (login, tokens, sessão) — isso é
                        responsabilidade do Auth Service, que consome os endpoints internos deste
                        serviço em `/internal/v1/users/**` para validar/atualizar credencial.

                        **Responsabilidades:**
                        - Cadastro de novos usuários com hash BCrypt de senha
                        - Consulta e atualização de perfil (`/api/v1/users/me`, `/api/v1/users/{id}`)
                        - CRUD de endereços de entrega
                        - Publicação do evento `user.created` no Kafka
                        - Endpoints internos (`/internal/v1/users/**`, não expostos pelo Gateway)
                          consumidos pelo Auth Service via OpenFeign

                        **Autenticação:** o cadastro é público. Os demais endpoints exigem cookie
                        `access_token` (httpOnly) válido, emitido pelo Auth Service.
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
                .description("JWT emitido por este serviço. Em produção é entregue como cookie httpOnly "
                        + "access_token; o esquema Bearer aqui existe apenas para testar via Swagger UI.");
    }

}
