package com.wersiv.datingsite.backend.user.mapper;

//TODO: complete mapper

import com.wersiv.datingsite.backend.user.dto.CreateUserRequest;
import com.wersiv.datingsite.backend.user.dto.UserResponse;
import com.wersiv.datingsite.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest createUserRequest);

    UserResponse toResponse(User user);
}
