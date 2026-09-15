package com.autohubstore.userservice.application.mapper;

import com.autohubstore.userservice.application.dto.request.AddressRequest;
import com.autohubstore.userservice.application.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Address toDomain(AddressRequest request);

}
