package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.mapper.CategoryMapper;
import com.autohubstore.catalogservice.exception.CategoryNotFoundException;
import com.autohubstore.catalogservice.exception.CategorySlugAlreadyExistsException;
import com.autohubstore.catalogservice.domain.projection.CategoryProductCountProjection;
import com.autohubstore.catalogservice.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new CategorySlugAlreadyExistsException(request.slug());
        }
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findCategories() {
        List<CategoryProductCountProjection> projections = categoryRepository.findProductCounts();

        Map<UUID, Long> productCounts = projections.stream()
                .collect(Collectors.toMap(
                        CategoryProductCountProjection::getCategoryId,
                        CategoryProductCountProjection::getProductCount
                ));

        return categoryRepository.findAllByOrderByCreatedAt().stream()
                .map(category -> categoryMapper.toResponse(category, productCounts.getOrDefault(category.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category findEntityOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
    }

}
