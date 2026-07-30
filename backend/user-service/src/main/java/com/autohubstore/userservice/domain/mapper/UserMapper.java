package com.autohubstore.userservice.domain.mapper;

import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "role", constant = "USER")
    User toEntity(CreateUserRequest request);

    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

}
