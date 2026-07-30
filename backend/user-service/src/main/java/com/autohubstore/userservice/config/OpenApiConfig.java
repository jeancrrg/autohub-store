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
                        Microsserviço responsável pelo cadastro, perfil e endereços de usuários do AutoHubStore.

                        **Responsabilidades:**
                        - Cadastro de novos usuários com hash BCrypt de senha
                        - Consulta e atualização de perfil
                        - CRUD de endereços de entrega
                        - Publicação do evento `user.created` no Kafka
                        - Endpoint interno `/internal/users/credentials` para o Auth Service

                        **Autenticação:** endpoints públicos não requerem token. Endpoints de perfil e endereço
                        requerem Bearer JWT emitido pelo Auth Service (validado pelo API Gateway).
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
