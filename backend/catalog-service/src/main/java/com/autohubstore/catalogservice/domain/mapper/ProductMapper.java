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
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "images", source = "images")
    ProductResponse toResponse(Product product, String categoryName, String brandName, String brandSlug,
                                List<ProductImageResponse> images);

    @Mapping(target = "status", ignore = true)
    Product toEntity(CreateProductRequest request, UUID categoryId, UUID brandId, String sku, String slug);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "brandId", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product product);

}
