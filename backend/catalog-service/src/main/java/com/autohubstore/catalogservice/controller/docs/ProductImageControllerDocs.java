package com.autohubstore.catalogservice.controller.docs;

import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Product Images", description = """
        Imagens de produtos, armazenadas no MinIO. Upload e remoção requerem
        Bearer JWT com role ADMIN (validado pelo API Gateway).
        """)
public interface ProductImageControllerDocs {

    @Operation(
            summary = "Enviar imagens do produto",
            description = "Envia uma ou mais imagens para um produto. Tipos aceitos: JPEG, PNG e WEBP, "
                    + "até 5MB cada. A primeira imagem enviada para um produto sem imagens vira a "
                    + "imagem primária (`isPrimary`) automaticamente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Imagens enviadas com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductImageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tipo de arquivo não suportado ou tamanho acima do limite",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(
            description = "Arquivos de imagem a enviar",
            required = true
    )
    ResponseEntity<List<ProductImageResponse>> uploadImages(
            @Parameter(description = "UUID do produto", required = true) UUID id,
            List<MultipartFile> files
    );

    @Operation(
            summary = "Remover imagem do produto",
            description = "Remove uma imagem do produto, tanto do MinIO quanto do banco de dados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagem removida com sucesso"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto ou imagem não encontrados",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> deleteImage(
            @Parameter(description = "UUID do produto", required = true) UUID id,
            @Parameter(description = "UUID da imagem", required = true) UUID imageId
    );

}
