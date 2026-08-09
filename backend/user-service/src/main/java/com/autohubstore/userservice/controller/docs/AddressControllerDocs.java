package com.autohubstore.userservice.controller.docs;

import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Addresses", description = """
        Gerenciamento dos endereços de entrega vinculados a um usuário.
        Todos os endpoints requerem Bearer JWT válido.
        Apenas um endereço pode ser marcado como padrão (`isDefault = true`) por vez.
        """)
@SecurityRequirement(name = "bearerAuth")
public interface AddressControllerDocs {

    @Operation(
            summary = "Listar endereços",
            description = "Retorna todos os endereços de entrega cadastrados para o usuário."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de endereços (pode ser vazia)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<List<AddressResponse>> findAddresses(
            @Parameter(description = "UUID do usuário", required = true) UUID userId
    );

    @Operation(
            summary = "Criar endereço de entrega",
            description = "Adiciona um novo endereço ao usuário. "
                    + "Se `isDefault = true`, o endereço padrão anterior é automaticamente desmarcado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Endereço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos (campos obrigatórios ausentes ou CEP mal formatado)",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @RequestBody(
            description = "Dados do endereço. CEP no formato `00000-000`.",
            required = true,
            content = @Content(schema = @Schema(implementation = AddressRequest.class))
    )
    ResponseEntity<AddressResponse> createAddress(
            @Parameter(description = "UUID do usuário", required = true) UUID userId,
            AddressRequest request
    );

    @Operation(
            summary = "Remover endereço",
            description = "Remove um endereço de entrega do usuário. "
                    + "O endereço deve pertencer ao usuário informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endereço removido com sucesso"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário ou endereço não encontrado, "
                            + "ou o endereço não pertence ao usuário",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<Void> deleteAddress(
            @Parameter(description = "UUID do usuário", required = true) UUID userId,
            @Parameter(description = "UUID do endereço", required = true) UUID addressId
    );

}
