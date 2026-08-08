package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.config.RedisConfig;
import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.mapper.ProductMapper;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.ProductSkuAlreadyExistsException;
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

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-|-$");
    private static final Pattern NON_ALPHANUMERIC_UPPER = Pattern.compile("[^A-Z0-9]");

    private static final int SLUG_FIRST_SUFFIX = 2;
    private static final int SKU_RANDOM_SUFFIX_LENGTH = 8;
    private static final int CATEGORY_PREFIX_LENGTH = 3;
    private static final String DEFAULT_SKU_PREFIX = "SKU";

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final CatalogEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryService.findEntityOrThrow(request.categoryId());

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSku(resolveSku(request.sku(), category));
        product.setSlug(generateUniqueSlug(request.name()));
        product = productRepository.save(product);

        eventPublisher.publishProductCreated(toChangedEvent(product));

        return productMapper.toResponse(product);
    }

    @Cacheable(cacheNames = RedisConfig.CACHE_PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productMapper.toResponse(findEntityOrThrow(id));
    }

    @Cacheable(cacheNames = RedisConfig.CACHE_PRODUCTS, key = "'slug:' + #slug")
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException(slug));
        return productMapper.toResponse(product);
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
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findEntityOrThrow(id);
        String previousName = product.getName();
        productMapper.updateEntityFromRequest(request, product);

        if (request.name() != null && !request.name().equals(previousName)) {
            product.setSlug(generateUniqueSlug(request.name()));
        }

        if (request.categoryId() != null) {
            product.setCategory(categoryService.findEntityOrThrow(request.categoryId()));
        }

        product = productRepository.save(product);
        eventPublisher.publishProductUpdated(toChangedEvent(product));

        return productMapper.toResponse(product);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_PRODUCTS, allEntries = true),
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

    private String resolveSku(String requestedSku, Category category) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            if (productRepository.existsBySku(requestedSku)) {
                throw new ProductSkuAlreadyExistsException(requestedSku);
            }
            return requestedSku;
        }
        return generateSku(category);
    }

    private String generateSku(Category category) {
        String filtered = NON_ALPHANUMERIC_UPPER.matcher(category.getName().toUpperCase(Locale.ROOT)).replaceAll("");
        String prefix = filtered.isEmpty()
                ? DEFAULT_SKU_PREFIX
                : filtered.substring(0, Math.min(CATEGORY_PREFIX_LENGTH, filtered.length()));

        String candidate;
        do {
            String randomPart = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, SKU_RANDOM_SUFFIX_LENGTH)
                    .toUpperCase(Locale.ROOT);
            candidate = prefix + "-" + randomPart;
        } while (productRepository.existsBySku(candidate));

        return candidate;
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = SLUG_FIRST_SUFFIX;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String slugify(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String lowerCased = withoutDiacritics.toLowerCase(Locale.ROOT);
        String hyphenated = NON_ALPHANUMERIC.matcher(lowerCased).replaceAll("-");
        return EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
    }

    private ProductChangedEvent toChangedEvent(Product product) {
        return new ProductChangedEvent(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getStatus(),
                null
        );
    }

}
