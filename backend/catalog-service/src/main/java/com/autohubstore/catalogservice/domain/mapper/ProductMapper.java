package com.autohubstore.catalogservice.domain.mapper;

import com.autohubstore.catalogservice.domain.dto.request.CreateProductRequest;
import com.autohubstore.catalogservice.domain.dto.request.UpdateProductRequest;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.domain.dto.response.ProductResponse;
import com.autohubstore.catalogservice.domain.entity.Product;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "images", source = "images")
    ProductResponse toResponse(Product product, String categoryName, List<ProductImageResponse> images);

    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "brandId", ignore = true)
    @Mapping(target = "status", ignore = true)
    Product toEntity(CreateProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "brandId", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product product);

}
