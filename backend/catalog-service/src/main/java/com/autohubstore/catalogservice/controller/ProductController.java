package com.autohubstore.catalogservice.controller;

import com.autohubstore.catalogservice.controller.docs.ProductControllerDocs;
import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.service.ProductImageService;
import com.autohubstore.catalogservice.service.ProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerDocs {

    private final ProductService productService;
    private final ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findProducts(@RequestParam(required = false) UUID categoryId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.findProducts(categoryId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findProduct(@PathVariable UUID id) {
        ProductResponse response = productService.findProduct(id);
        productService.publishProductViewed(response);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> findProductBySlug(@PathVariable String slug) {
        ProductResponse response = productService.findProductBySlug(slug);
        productService.publishProductViewed(response);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/images/{id}")
    public ResponseEntity<List<ProductImageResponse>> uploadImages(
            @PathVariable UUID id, @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.uploadImages(id, files));
    }

    @DeleteMapping("/images/{id}/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productImageService.deleteImage(id, imageId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
