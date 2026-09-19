package com.autohubstore.catalogservice.unit.service;

import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.mapper.CategoryMapper;
import com.autohubstore.catalogservice.domain.projection.CategoryProductCountProjection;
import com.autohubstore.catalogservice.exception.CategoryNotFoundException;
import com.autohubstore.catalogservice.exception.CategorySlugAlreadyExistsException;
import com.autohubstore.catalogservice.repository.CategoryRepository;
import com.autohubstore.catalogservice.service.CategoryService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID CATEGORY_WITHOUT_PRODUCTS_ID = UUID.randomUUID();
    private static final String CATEGORY_NAME = "Freios";
    private static final String CATEGORY_SLUG = "freios";
    private static final long PRODUCT_COUNT = 3L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Deve criar categoria quando o slug ainda nao estiver cadastrado")
    void shouldCreateCategoryWhenSlugDoesNotExist() {
        CreateCategoryRequest request = new CreateCategoryRequest(CATEGORY_NAME, CATEGORY_SLUG);
        Category category = Category.builder().id(CATEGORY_ID).name(CATEGORY_NAME).slug(CATEGORY_SLUG).build();
        CategoryResponse response = new CategoryResponse(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG, 0L, null);

        when(categoryRepository.existsBySlug(CATEGORY_SLUG)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve lancar CategorySlugAlreadyExistsException quando o slug ja estiver cadastrado")
    void shouldThrowSlugAlreadyExistsExceptionWhenSlugIsDuplicated() {
        CreateCategoryRequest request = new CreateCategoryRequest(CATEGORY_NAME, CATEGORY_SLUG);
        when(categoryRepository.existsBySlug(CATEGORY_SLUG)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CategorySlugAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar contagem de produtos zero quando a categoria nao tiver produto na projecao")
    void shouldReturnZeroProductCountWhenCategoryHasNoProjection() {
        Category withProducts = Category.builder().id(CATEGORY_ID).name(CATEGORY_NAME).slug(CATEGORY_SLUG).build();
        Category withoutProducts = Category.builder()
                .id(CATEGORY_WITHOUT_PRODUCTS_ID).name("Suspensao").slug("suspensao").build();
        CategoryProductCountProjection projection = projectionOf(CATEGORY_ID, PRODUCT_COUNT);

        when(categoryRepository.findProductCounts()).thenReturn(List.of(projection));
        when(categoryRepository.findAllByOrderByCreatedAt()).thenReturn(List.of(withProducts, withoutProducts));
        when(categoryMapper.toResponse(withProducts, PRODUCT_COUNT))
                .thenReturn(new CategoryResponse(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG, PRODUCT_COUNT, null));
        when(categoryMapper.toResponse(withoutProducts, 0L))
                .thenReturn(new CategoryResponse(CATEGORY_WITHOUT_PRODUCTS_ID, "Suspensao", "suspensao", 0L, null));

        List<CategoryResponse> result = categoryService.findCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).productCount()).isEqualTo(PRODUCT_COUNT);
        assertThat(result.get(1).productCount()).isZero();
    }

    @Test
    @DisplayName("Deve retornar a categoria quando o id existir")
    void shouldReturnCategoryWhenIdExists() {
        Category category = Category.builder().id(CATEGORY_ID).name(CATEGORY_NAME).slug(CATEGORY_SLUG).build();
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        Category result = categoryService.findEntityOrThrow(CATEGORY_ID);

        assertThat(result).isEqualTo(category);
    }

    @Test
    @DisplayName("Deve lancar CategoryNotFoundException quando a categoria nao existir")
    void shouldThrowCategoryNotFoundExceptionWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findEntityOrThrow(CATEGORY_ID))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    private CategoryProductCountProjection projectionOf(UUID categoryId, Long productCount) {
        return new CategoryProductCountProjection() {
            @Override
            public UUID getCategoryId() {
                return categoryId;
            }

            @Override
            public Long getProductCount() {
                return productCount;
            }
        };
    }

}
