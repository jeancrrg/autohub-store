package com.autohubstore.userservice.domain.mapper;

import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    @Mapping(target = "userId", ignore = true)
    Address toEntity(AddressRequest request);

}
