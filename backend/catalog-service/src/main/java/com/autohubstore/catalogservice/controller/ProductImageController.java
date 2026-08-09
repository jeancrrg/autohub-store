package com.autohubstore.catalogservice.controller;

import com.autohubstore.catalogservice.controller.docs.ProductImageControllerDocs;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.service.ProductImageService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/product-images")
@RequiredArgsConstructor
public class ProductImageController implements ProductImageControllerDocs {

    private final ProductImageService productImageService;

    @PostMapping("/{id}")
    public ResponseEntity<List<ProductImageResponse>> uploadImages(
            @PathVariable UUID id, @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.uploadImages(id, files));
    }

    @DeleteMapping("/{id}/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productImageService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

}
