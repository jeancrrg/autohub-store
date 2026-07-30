package com.autohubstore.userservice.controller.docs;

import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdatePasswordRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.userservice.domain.dto.response.ExistsResponse;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Users", description = """
        Gerenciamento de cadastro e perfil de usuários.
        O endpoint de cadastro (`POST /api/v1/users`) é público.
        Os demais requerem Bearer JWT válido (validado pelo API Gateway).
        """)
public interface UserControllerDocs {

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cria uma nova conta de usuário. A senha é armazenada com hash BCrypt. "
                    + "Após o cadastro, o evento `user.created` é publicado no Kafka."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos (campos obrigatórios ausentes ou mal formatados)",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "Dados do novo usuário",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
    )
    ResponseEntity<UserResponse> createUser(CreateUserRequest request);

    @Operation(
            summary = "Buscar perfil por ID",
            description = "Retorna os dados do perfil de um usuário pelo seu UUID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<UserResponse> getUser(
            @Parameter(description = "UUID do usuário", required = true) UUID id
    );

    @Operation(
            summary = "Atualizar perfil",
            description = "Atualiza os dados mutáveis do perfil (nome completo)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil atualizado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(
            description = "Campos a atualizar",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdateUserRequest.class))
    )
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "UUID do usuário", required = true) UUID id,
            UpdateUserRequest request
    );

    @Operation(
            summary = "Buscar usuário por e-mail",
            description = "Endpoint de uso interno — chamado pelo Auth Service via OpenFeign. "
                    + "Retorna os dados completos do usuário, incluindo roles."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nenhum usuário com este e-mail",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "E-mail do usuário", required = true, example = "user@example.com")
            String email
    );

    @Operation(
            summary = "Verificar existência de e-mail",
            description = "Endpoint de uso interno — chamado pelo Auth Service para verificar "
                    + "se um e-mail existe antes de iniciar o fluxo de reset de senha."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado da verificação",
                    content = @Content(schema = @Schema(implementation = ExistsResponse.class))
            )
    })
    ResponseEntity<ExistsResponse> existsByEmail(
            @Parameter(description = "E-mail a verificar", required = true, example = "user@example.com")
            String email
    );

    @Operation(
            summary = "Validar credenciais",
            description = "Endpoint de uso interno — chamado pelo Auth Service no fluxo de login. "
                    + "Verifica e-mail e senha (BCrypt). Retorna os dados do usuário se válidos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Credenciais válidas — retorna dados do usuário",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Conta inativa ou bloqueada",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "E-mail e senha em texto plano",
            required = true,
            content = @Content(schema = @Schema(implementation = ValidateCredentialsRequest.class))
    )
    ResponseEntity<UserResponse> validateCredentials(ValidateCredentialsRequest request);

    @Operation(
            summary = "Atualizar senha",
            description = "Endpoint de uso interno — chamado pelo Auth Service após o usuário "
                    + "confirmar o reset de senha. A nova senha é armazenada com hash BCrypt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha atualizada com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nova senha inválida (mínimo 8 caracteres)",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "Nova senha em texto plano (mínimo 8 caracteres)",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdatePasswordRequest.class))
    )
    ResponseEntity<Void> updatePassword(
            @Parameter(description = "UUID do usuário", required = true) UUID id,
            UpdatePasswordRequest request
    );

}
