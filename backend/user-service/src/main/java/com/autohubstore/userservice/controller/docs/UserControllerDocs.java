package com.autohubstore.userservice.controller.docs;

import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdatePasswordRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
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
        Os demais requerem cookie access_token válido.
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
            summary = "Buscar perfil do usuário autenticado",
            description = "Retorna os dados do usuário identificado pelo cookie access_token da requisição."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Cookie access_token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<UserResponse> findCurrentUser();

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
    ResponseEntity<UserResponse> findUser(
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
            summary = "Atualizar senha",
            description = "Atualiza a senha do usuário (usado internamente pelo fluxo de reset de senha, "
                    + "também disponível como endpoint administrativo). A nova senha é armazenada com hash BCrypt."
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
