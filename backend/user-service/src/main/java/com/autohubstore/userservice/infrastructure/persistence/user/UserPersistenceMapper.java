package com.autohubstore.userservice.infrastructure.persistence.user;

import com.autohubstore.userservice.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    UserJpaEntity toJpaEntity(User user);

    User toDomain(UserJpaEntity entity);

}
