package com.digitalbank.user.application.mapper;

import com.digitalbank.user.application.dto.CreateUserRequest;
import com.digitalbank.user.application.dto.UserResponse;
import com.digitalbank.user.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    // status and timestamps are managed by the entity itself
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "status",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    // Updates only non-null fields in the existing entity
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "cpf",       ignore = true)
    @Mapping(target = "status",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(com.digitalbank.user.application.dto.UpdateUserRequest request, @MappingTarget User user);
}
