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
                                .url("http://localhost:8002")
                                .description("Desenvolvimento local"),
                        new Server()
                                .url("http://user-service:8002")
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
                        Microsserviço responsável por identidade, cadastro, perfil e endereços de usuários
                        do AutoHubStore (fusão Auth + User).

                        **Responsabilidades:**
                        - Login, logout, refresh e reset de senha (JWT + refresh token + blacklist Redis)
                        - Cadastro de novos usuários com hash BCrypt de senha
                        - Consulta e atualização de perfil (`/api/v1/users/me`, `/api/v1/users/{id}`)
                        - CRUD de endereços de entrega
                        - Publicação dos eventos `user.created` e `user.password-reset` no Kafka

                        **Autenticação:** endpoints de cadastro e `/api/v1/auth/*` são públicos. Os demais
                        exigem cookie `access_token` (httpOnly) válido, emitido por este próprio serviço.
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
