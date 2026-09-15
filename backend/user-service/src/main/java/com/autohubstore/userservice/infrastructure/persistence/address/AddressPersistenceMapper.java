package com.autohubstore.userservice.infrastructure.persistence.address;

import com.autohubstore.userservice.domain.model.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressPersistenceMapper {

    AddressJpaEntity toJpaEntity(Address address);

    Address toDomain(AddressJpaEntity entity);

}
