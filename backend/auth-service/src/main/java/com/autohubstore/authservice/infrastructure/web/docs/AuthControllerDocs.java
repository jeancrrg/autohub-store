package com.autohubstore.authservice.infrastructure.web.docs;

import com.autohubstore.authservice.application.dto.request.ForgotPasswordRequest;
import com.autohubstore.authservice.application.dto.request.LoginRequest;
import com.autohubstore.authservice.application.dto.request.ResetPasswordRequest;

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

@Tag(name = "Auth", description = """
        Autenticação e gerenciamento de tokens. Sessão é mantida via cookies httpOnly
        (`access_token` e `refresh_token`) — nenhum token é retornado no corpo das respostas.
        """)
public interface AuthControllerDocs {

    @Operation(
            summary = "Login",
            description = "Autentica com e-mail/senha e seta cookies httpOnly de sessão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso, cookies de sessão setados"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "Credenciais de login",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
    )
    ResponseEntity<Void> login(LoginRequest request);

    @Operation(
            summary = "Logout",
            description = "Revoga tokens e limpa cookies de sessão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão encerrada, cookies expirados")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> logout(
            @Parameter(description = "Access token via cookie httpOnly") String accessToken,
            @Parameter(description = "Refresh token via cookie httpOnly") String refreshToken
    );

    @Operation(
            summary = "Refresh token",
            description = "Rotaciona o refresh token e re-seta os cookies de sessão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rotacionados com sucesso"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token inválido ou expirado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<Void> refresh(
            @Parameter(description = "Refresh token via cookie httpOnly", required = true) String refreshToken
    );

    @Operation(
            summary = "Forgot password",
            description = "Solicita e-mail de reset de senha."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação aceita")
    })
    @RequestBody(
            description = "E-mail da conta",
            required = true,
            content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class))
    )
    ResponseEntity<Void> forgotPassword(ForgotPasswordRequest request);

    @Operation(
            summary = "Reset password",
            description = "Confirma reset de senha com token temporário."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token temporário inválido ou expirado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "Token temporário e nova senha",
            required = true,
            content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class))
    )
    ResponseEntity<Void> resetPassword(ResetPasswordRequest request);

}
