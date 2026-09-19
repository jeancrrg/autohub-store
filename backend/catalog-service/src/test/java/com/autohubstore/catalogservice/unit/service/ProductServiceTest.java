package com.autohubstore.catalogservice.unit.service;

import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.domain.entity.Brand;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.enums.ProductStatus;
import com.autohubstore.catalogservice.domain.mapper.ProductMapper;
import com.autohubstore.catalogservice.exception.BrandNotFoundException;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.ProductSkuAlreadyExistsException;
import com.autohubstore.catalogservice.messaging.CatalogEventPublisher;
import com.autohubstore.catalogservice.messaging.ProductChangedEvent;
import com.autohubstore.catalogservice.messaging.ProductViewedEvent;
import com.autohubstore.catalogservice.repository.ProductRepository;
import com.autohubstore.catalogservice.service.BrandService;
import com.autohubstore.catalogservice.service.CategoryService;
import com.autohubstore.catalogservice.service.ProductImageService;
import com.autohubstore.catalogservice.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID BRAND_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String CATEGORY_NAME = "Motor";
    private static final String CATEGORY_SLUG = "motor";
    private static final String BRAND_NAME = "NGK";
    private static final String BRAND_SLUG = "ngk";
    private static final String PRODUCT_NAME = "Vela de Ignicao NGK";
    private static final BigDecimal PRODUCT_PRICE = BigDecimal.valueOf(120);
    private static final BigDecimal UPDATED_PRICE = BigDecimal.valueOf(200);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BrandService brandService;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CatalogEventPublisher eventPublisher;

    private ProductService productService;
    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService, brandService,
                productImageService, productMapper, eventPublisher);
        category = Category.builder().id(CATEGORY_ID).name(CATEGORY_NAME).slug(CATEGORY_SLUG).build();
        brand = Brand.builder().id(BRAND_ID).name(BRAND_NAME).slug(BRAND_SLUG).build();
    }

    @Test
    @DisplayName("Deve retornar o produto quando o id existir")
    void shouldReturnProductWhenIdExists() {
        Product product = existingProduct();
        ProductResponse expectedResponse = productResponseFor(product);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productImageService.findImages(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productMapper.toResponse(product, CATEGORY_NAME, BRAND_NAME, BRAND_SLUG, Collections.emptyList()))
                .thenReturn(expectedResponse);

        ProductResponse result = productService.findProduct(PRODUCT_ID);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Deve lancar ProductNotFoundException quando o produto nao existir")
    void shouldThrowProductNotFoundExceptionWhenProductDoesNotExist() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProduct(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Deve criar produto quando categoria e marca existirem e o sku informado for unico")
    void shouldCreateProductWhenSkuIsUnique() {
        CreateProductRequest request = new CreateProductRequest(PRODUCT_NAME, "FLT-01", "descricao",
                PRODUCT_PRICE, CATEGORY_ID, BRAND_ID);
        Product entityToSave = existingProduct();
        ProductResponse expectedResponse = productResponseFor(entityToSave);

        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productRepository.existsBySku("FLT-01")).thenReturn(false);
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productMapper.toEntity(eq(request), eq(CATEGORY_ID), eq(BRAND_ID), eq("FLT-01"), anyString()))
                .thenReturn(entityToSave);
        when(productRepository.save(entityToSave)).thenReturn(entityToSave);
        when(productImageService.findImages(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productMapper.toResponse(entityToSave, CATEGORY_NAME, BRAND_NAME, BRAND_SLUG, Collections.emptyList()))
                .thenReturn(expectedResponse);

        ProductResponse result = productService.createProduct(request);

        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<ProductChangedEvent> eventCaptor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(eventPublisher).publishProductCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(eventCaptor.getValue().categoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("Deve gerar sku automaticamente quando o sku informado estiver em branco")
    void shouldGenerateSkuWhenRequestOmitsIt() {
        CreateProductRequest request = new CreateProductRequest(PRODUCT_NAME, null, "descricao",
                PRODUCT_PRICE, CATEGORY_ID, BRAND_ID);
        Product entityToSave = existingProduct();

        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productMapper.toEntity(eq(request), eq(CATEGORY_ID), eq(BRAND_ID), anyString(), anyString()))
                .thenReturn(entityToSave);
        when(productRepository.save(entityToSave)).thenReturn(entityToSave);
        when(productImageService.findImages(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productMapper.toResponse(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(productResponseFor(entityToSave));

        productService.createProduct(request);

        ArgumentCaptor<String> skuCaptor = ArgumentCaptor.forClass(String.class);
        verify(productMapper).toEntity(eq(request), eq(CATEGORY_ID), eq(BRAND_ID), skuCaptor.capture(), anyString());
        assertThat(skuCaptor.getValue()).startsWith("MOT-");
    }

    @Test
    @DisplayName("Deve lancar ProductSkuAlreadyExistsException quando o sku informado ja estiver cadastrado")
    void shouldThrowSkuAlreadyExistsExceptionWhenSkuIsDuplicated() {
        CreateProductRequest request = new CreateProductRequest(PRODUCT_NAME, "FLT-01", "descricao",
                PRODUCT_PRICE, CATEGORY_ID, BRAND_ID);

        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productRepository.existsBySku("FLT-01")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ProductSkuAlreadyExistsException.class);

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishProductCreated(any());
    }

    @Test
    @DisplayName("Deve lancar BrandNotFoundException quando a marca informada nao existir")
    void shouldThrowBrandNotFoundExceptionWhenBrandDoesNotExist() {
        CreateProductRequest request = new CreateProductRequest(PRODUCT_NAME, "FLT-01", "descricao",
                PRODUCT_PRICE, CATEGORY_ID, BRAND_ID);

        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenThrow(new BrandNotFoundException(BRAND_ID.toString()));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BrandNotFoundException.class);

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishProductCreated(any());
    }

    @Test
    @DisplayName("Deve regerar o slug quando o nome do produto for alterado na atualizacao")
    void shouldRegenerateSlugWhenNameChangesOnUpdate() {
        Product product = existingProduct();
        UpdateProductRequest request = new UpdateProductRequest("Novo Nome do Produto", null, null, null, null, null);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);
        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productImageService.findImages(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productMapper.toResponse(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(productResponseFor(product));

        productService.updateProduct(PRODUCT_ID, request);

        assertThat(product.getSlug()).isEqualTo("novo-nome-do-produto");
        verify(eventPublisher).publishProductUpdated(any());
    }

    @Test
    @DisplayName("Deve manter o slug quando a atualizacao nao alterar o nome do produto")
    void shouldKeepSlugWhenNameIsNotChangedOnUpdate() {
        Product product = existingProduct();
        String originalSlug = product.getSlug();
        UpdateProductRequest request = new UpdateProductRequest(null, null, UPDATED_PRICE, null, null, null);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(categoryService.findEntityOrThrow(CATEGORY_ID)).thenReturn(category);
        when(brandService.findEntityOrThrow(BRAND_ID)).thenReturn(brand);
        when(productImageService.findImages(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productMapper.toResponse(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(productResponseFor(product));

        productService.updateProduct(PRODUCT_ID, request);

        assertThat(product.getSlug()).isEqualTo(originalSlug);
    }

    @Test
    @DisplayName("Deve remover o produto quando ele existir")
    void shouldDeleteProductWhenItExists() {
        Product product = existingProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        productService.deleteProduct(PRODUCT_ID);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("Deve lancar ProductNotFoundException ao remover produto inexistente")
    void shouldThrowProductNotFoundExceptionWhenDeletingNonexistentProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve publicar evento de produto visualizado com o id e nome do produto")
    void shouldPublishProductViewedEvent() {
        ProductResponse response = new ProductResponse(PRODUCT_ID, "FLT-01", "slug", PRODUCT_NAME, "desc",
                PRODUCT_PRICE, CATEGORY_ID, CATEGORY_NAME, BRAND_ID, BRAND_NAME, BRAND_SLUG,
                ProductStatus.ACTIVE, List.of(), null, null);

        productService.publishProductViewed(response);

        ArgumentCaptor<ProductViewedEvent> captor = ArgumentCaptor.forClass(ProductViewedEvent.class);
        verify(eventPublisher).publishProductViewed(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(captor.getValue().productName()).isEqualTo(PRODUCT_NAME);
    }

    private Product existingProduct() {
        return Product.builder()
                .id(PRODUCT_ID)
                .sku("FLT-01")
                .slug("vela-de-ignicao-ngk")
                .name(PRODUCT_NAME)
                .description("descricao")
                .price(PRODUCT_PRICE)
                .categoryId(CATEGORY_ID)
                .brandId(BRAND_ID)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private ProductResponse productResponseFor(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getSlug(), product.getName(),
                product.getDescription(), product.getPrice(), product.getCategoryId(), CATEGORY_NAME,
                product.getBrandId(), BRAND_NAME, BRAND_SLUG, product.getStatus(), List.of(), null, null);
    }

}
