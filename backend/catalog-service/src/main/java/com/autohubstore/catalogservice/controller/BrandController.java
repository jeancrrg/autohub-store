package com.autohubstore.catalogservice.controller;

import com.autohubstore.catalogservice.controller.docs.BrandControllerDocs;
import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.service.BrandService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/brands")
@RequiredArgsConstructor
public class BrandController implements BrandControllerDocs {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> findBrands() {
        return ResponseEntity.status(HttpStatus.OK).body(brandService.findBrands());
    }

}
