package com.autohubstore.userservice.infrastructure.persistence.address;

import com.autohubstore.userservice.domain.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressPersistenceMapper {

    @Mapping(target = "isDefault", source = "default")
    AddressJpaEntity toJpaEntity(Address address);

    @Mapping(target = "isDefault", source = "default")
    Address toDomain(AddressJpaEntity entity);

}
