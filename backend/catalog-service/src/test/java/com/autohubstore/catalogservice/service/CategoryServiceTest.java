package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.mapper.CategoryMapper;
import com.autohubstore.catalogservice.exception.CategoryNotFoundException;
import com.autohubstore.catalogservice.exception.CategorySlugAlreadyExistsException;
import com.autohubstore.catalogservice.repository.CategoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
        categoryService = new CategoryService(categoryRepository, categoryMapper);
    }

    @Test
    void createCategory_shouldPersistCategoryWithoutParent() {
        CreateCategoryRequest request = new CreateCategoryRequest("Filtros", "filtros", null);

        when(categoryRepository.existsBySlug("filtros")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.name()).isEqualTo("Filtros");
        assertThat(response.slug()).isEqualTo("filtros");
        assertThat(response.parentId()).isNull();
    }

    @Test
    void createCategory_shouldThrowWhenSlugAlreadyExists() {
        CreateCategoryRequest request = new CreateCategoryRequest("Filtros", "filtros", null);

        when(categoryRepository.existsBySlug("filtros")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CategorySlugAlreadyExistsException.class)
                .hasMessageContaining("filtros");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_shouldLinkParentWhenParentIdProvided() {
        UUID parentId = UUID.randomUUID();
        Category parent = Category.builder().id(parentId).name("Peças").slug("pecas").build();
        CreateCategoryRequest request = new CreateCategoryRequest("Filtros", "filtros", parentId);

        when(categoryRepository.existsBySlug("filtros")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.parentId()).isEqualTo(parentId);
    }

    @Test
    void findEntityOrThrow_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findEntityOrThrow(id))
                .isInstanceOf(CategoryNotFoundException.class);
    }

}
