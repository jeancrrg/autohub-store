package com.autohubstore.catalogservice.controller.docs;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Brands", description = """
        Marcas de produtos. Leitura pública, sem escrita nesta versão.
        """)
public interface BrandControllerDocs {

    @Operation(
            summary = "Listar marcas",
            description = "Retorna todas as marcas cadastradas, ordenadas alfabeticamente por `name`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de marcas retornada com sucesso")
    })
    ResponseEntity<List<BrandResponse>> listBrands();

}
