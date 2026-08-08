package com.autohubstore.catalogservice.domain.mapper;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.domain.entity.Brand;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    BrandResponse toResponse(Brand brand);

}
