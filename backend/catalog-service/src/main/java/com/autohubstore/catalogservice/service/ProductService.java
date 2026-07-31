package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.config.RedisConfig;
import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.mapper.ProductMapper;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.messaging.CatalogEventPublisher;
import com.autohubstore.catalogservice.messaging.ProductChangedEvent;
import com.autohubstore.catalogservice.messaging.ProductViewedEvent;
import com.autohubstore.catalogservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final CatalogEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryService.findEntityOrThrow(request.categoryId());

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product = productRepository.save(product);

        eventPublisher.publishProductCreated(toChangedEvent(product));

        return productMapper.toResponse(product);
    }

    @Cacheable(cacheNames = RedisConfig.CACHE_PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productMapper.toResponse(findEntityOrThrow(id));
    }

    public void publishProductViewed(ProductResponse product) {
        eventPublisher.publishProductViewed(
                new ProductViewedEvent(product.id(), product.name(), Instant.now()));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Cacheable(cacheNames = RedisConfig.CACHE_PRODUCTS_BY_CATEGORY,
            key = "#categoryId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProductsByCategory(UUID categoryId, Pageable pageable) {
        categoryService.findEntityOrThrow(categoryId);
        return productRepository.findByCategoryId(categoryId, pageable).map(productMapper::toResponse);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findEntityOrThrow(id);
        productMapper.updateEntityFromRequest(request, product);

        if (request.categoryId() != null) {
            product.setCategory(categoryService.findEntityOrThrow(request.categoryId()));
        }

        product = productRepository.save(product);
        eventPublisher.publishProductUpdated(toChangedEvent(product));

        return productMapper.toResponse(product);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = findEntityOrThrow(id);
        productRepository.delete(product);
    }

    private Product findEntityOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
    }

    private ProductChangedEvent toChangedEvent(Product product) {
        return new ProductChangedEvent(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getStatus(),
                product.getStockQuantity()
        );
    }

}
