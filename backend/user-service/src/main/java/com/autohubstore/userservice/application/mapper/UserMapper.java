package com.autohubstore.userservice.application.mapper;

import com.autohubstore.userservice.application.dto.request.CreateUserRequest;
import com.autohubstore.userservice.application.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toDomain(CreateUserRequest request);

    void updateDomainFromRequest(UpdateUserRequest request, @MappingTarget User user);

}
