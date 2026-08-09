package com.autohubstore.userservice.controller.docs;

import com.autohubstore.userservice.domain.dto.request.ForgotPasswordRequest;
import com.autohubstore.userservice.domain.dto.request.LoginRequest;
import com.autohubstore.userservice.domain.dto.request.ResetPasswordRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = """
        Login, logout, refresh e reset de senha. Access e refresh token são entregues
        como cookies httpOnly — nenhum endpoint aqui devolve o token no corpo da resposta.
        """)
public interface AuthControllerDocs {

    @Operation(summary = "Login", description = "Valida credenciais e emite access/refresh token como cookies httpOnly.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login efetuado"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Conta inativa ou bloqueada",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    @RequestBody(description = "E-mail e senha", required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class)))
    ResponseEntity<Void> login(LoginRequest request);

    @Operation(summary = "Logout", description = "Coloca o access token na blacklist (Redis) e revoga o refresh token.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Logout efetuado"))
    ResponseEntity<Void> logout(String accessToken, String refreshToken);

    @Operation(summary = "Refresh", description = "Rotaciona o refresh token e emite um novo access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    ResponseEntity<Void> refresh(String refreshToken);

    @Operation(summary = "Esqueci minha senha",
            description = "Sempre retorna 202, mesmo se o e-mail não existir, para não vazar dados cadastrados.")
    @ApiResponses(@ApiResponse(responseCode = "202", description = "Solicitação aceita"))
    @RequestBody(description = "E-mail cadastrado", required = true,
            content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class)))
    ResponseEntity<Void> forgotPassword(ForgotPasswordRequest request);

    @Operation(summary = "Resetar senha", description = "Confirma o reset com o token temporário recebido por e-mail.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha atualizada"),
            @ApiResponse(responseCode = "401", description = "Token inválido, expirado ou já utilizado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    @RequestBody(description = "Token e nova senha", required = true,
            content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
    ResponseEntity<Void> resetPassword(ResetPasswordRequest request);

}
