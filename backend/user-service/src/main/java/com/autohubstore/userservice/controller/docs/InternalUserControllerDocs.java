package com.autohubstore.userservice.controller.docs;

import com.autohubstore.userservice.domain.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Internal", description = """
        Endpoints de comunicação interna entre microsserviços.
        **Não devem ser chamados diretamente pelo cliente final.**
        Acesso via rede interna Docker Compose / Kubernetes.
        Estes endpoints seguem a especificação da rota `/internal/users/credentials`.
        """)
public interface InternalUserControllerDocs {

    @Operation(
            summary = "Buscar credenciais por e-mail",
            description = """
                    Endpoint de uso exclusivo do Auth Service (chamado via OpenFeign).
                    Retorna os dados completos do usuário — incluindo `roles` para geração do JWT —
                    a partir do e-mail informado como query parameter.

                    **Especificação:** `GET /internal/users/credentials?email={email}`
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário encontrado — retorna perfil completo com roles",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetro `email` ausente ou vazio",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nenhum usuário cadastrado com este e-mail",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<UserResponse> getCredentials(
            @Parameter(
                    description = "E-mail do usuário a consultar",
                    required = true,
                    example = "usuario@autohubstore.com"
            )
            String email
    );
}
