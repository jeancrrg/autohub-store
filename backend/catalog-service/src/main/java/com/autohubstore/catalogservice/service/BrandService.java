package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.domain.entity.Brand;
import com.autohubstore.catalogservice.domain.mapper.BrandMapper;
import com.autohubstore.catalogservice.exception.BrandNotFoundException;
import com.autohubstore.catalogservice.repository.BrandRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> findBrands() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .map(brandMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Brand findEntityOrThrow(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id.toString()));
    }

}
