package com.autohubstore.catalogservice.controller.docs;

import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Products", description = """
        Catálogo de produtos. Endpoints de leitura são públicos e usam cache Redis.
        Endpoints de escrita requerem Bearer JWT com role ADMIN (validado pelo API Gateway).
        """)
public interface ProductControllerDocs {

    @Operation(
            summary = "Listar produtos",
            description = "Lista produtos de forma paginada. Filtro opcional por categoria via `categoryId`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos retornada com sucesso")
    })
    ResponseEntity<Page<ProductResponse>> listProducts(
            @Parameter(description = "UUID da categoria para filtrar") UUID categoryId,
            Pageable pageable
    );

    @Operation(
            summary = "Detalhes do produto",
            description = "Retorna os dados de um produto. Resultado é cacheado no Redis (TTL 5 minutos) "
                    + "e publica o evento `catalog.product-viewed` no Kafka a cada chamada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto encontrado",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "UUID do produto", required = true) UUID id
    );

    @Operation(
            summary = "Criar produto",
            description = "Cria um novo produto. Publica o evento `catalog.product-created` no Kafka."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Produto criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Categoria informada não existe",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(
            description = "Dados do novo produto",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateProductRequest.class))
    )
    ResponseEntity<ProductResponse> createProduct(CreateProductRequest request);

    @Operation(
            summary = "Atualizar produto",
            description = "Atualiza campos do produto. Invalida o cache do produto e das listagens por "
                    + "categoria. Publica o evento `catalog.product-updated` no Kafka."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto atualizado",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto ou categoria não encontrados",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(
            description = "Campos a atualizar",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdateProductRequest.class))
    )
    ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "UUID do produto", required = true) UUID id,
            UpdateProductRequest request
    );

    @Operation(
            summary = "Remover produto",
            description = "Remove um produto e invalida seu cache."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto removido com sucesso"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> deleteProduct(
            @Parameter(description = "UUID do produto", required = true) UUID id
    );

}
