package com.autohubstore.catalogservice.controller.docs;

import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = """
        Categorias de produtos. Leitura pública, escrita requer
        Bearer JWT com role ADMIN (validado pelo API Gateway).
        """)
public interface CategoryControllerDocs {

    @Operation(
            summary = "Listar categorias",
            description = "Retorna todas as categorias, cada uma com `productCount` (total de produtos da "
                    + "categoria, independente de status ou estoque)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorias retornada com sucesso")
    })
    ResponseEntity<List<CategoryResponse>> findCategories();

    @Operation(
            summary = "Criar categoria",
            description = "Cria uma nova categoria."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Categoria criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Slug já cadastrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(
            description = "Dados da nova categoria",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateCategoryRequest.class))
    )
    ResponseEntity<CategoryResponse> createCategory(CreateCategoryRequest request);

    @Operation(
            summary = "Listar produtos por categoria",
            description = "Lista, de forma paginada, os produtos pertencentes a uma categoria. "
                    + "Resultado é cacheado no Redis (TTL 2 minutos)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos retornada com sucesso"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Categoria não encontrada",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    ResponseEntity<Page<ProductResponse>> findProductsByCategory(
            @Parameter(description = "UUID da categoria", required = true) UUID id,
            Pageable pageable
    );

}
