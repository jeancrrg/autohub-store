package com.autohubstore.catalogservice.controller;

import com.autohubstore.catalogservice.controller.docs.CategoryControllerDocs;
import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.service.CategoryService;
import com.autohubstore.catalogservice.service.ProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findCategories() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findCategories());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<Page<ProductResponse>> findProductsByCategory(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.findProductsByCategory(id, pageable));
    }

}
