package com.autohubstore.catalogservice.domain.mapper;

import com.autohubstore.catalogservice.domain.dto.request.CreateCategoryRequest;
import com.autohubstore.catalogservice.domain.dto.response.CategoryResponse;
import com.autohubstore.catalogservice.domain.entity.Category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "productCount", constant = "0L")
    CategoryResponse toResponse(Category category);

    CategoryResponse toResponse(Category category, Long productCount);

    Category toEntity(CreateCategoryRequest request);

}
