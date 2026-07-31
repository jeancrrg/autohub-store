package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.enums.ProductStatus;
import com.autohubstore.catalogservice.domain.mapper.ProductMapper;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.messaging.CatalogEventPublisher;
import com.autohubstore.catalogservice.messaging.ProductChangedEvent;
import com.autohubstore.catalogservice.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CatalogEventPublisher eventPublisher;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
        productService = new ProductService(productRepository, categoryService, productMapper, eventPublisher);
    }

    @Test
    void createProduct_shouldPersistProductAndPublishEvent() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Filtros").slug("filtros").build();
        CreateProductRequest request = new CreateProductRequest(
                "Filtro de Ar K&N", "Filtro de alto desempenho", new BigDecimal("299.90"), 50, categoryId);

        when(categoryService.findEntityOrThrow(categoryId)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertThat(response.name()).isEqualTo("Filtro de Ar K&N");
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Filtros");

        ArgumentCaptor<ProductChangedEvent> eventCaptor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(eventPublisher).publishProductCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().name()).isEqualTo("Filtro de Ar K&N");
    }

    @Test
    void getProduct_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProduct_shouldReturnMappedResponse() {
        UUID id = UUID.randomUUID();
        Category category = Category.builder().id(UUID.randomUUID()).name("Filtros").slug("filtros").build();
        Product product = Product.builder()
                .id(id)
                .name("Vela de Ignição")
                .price(new BigDecimal("49.90"))
                .stockQuantity(10)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Vela de Ignição");
    }

}
