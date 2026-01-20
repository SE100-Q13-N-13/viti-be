package com.example.viti_be.mapper;

import com.example.viti_be.dto.request.RoleRequest;
import com.example.viti_be.dto.response.RoleResponse;
import com.example.viti_be.model.Role;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct Mapper cho Role entities
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    /**
     * Map RoleRequest -> Role entity (cho tạo mới)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Role toEntity(RoleRequest request);

    /**
     * Map Role entity -> RoleResponse
     */
    @Mapping(target = "userCount", ignore = true)
    RoleResponse toResponse(Role role);

    /**
     * Map List<Role> -> List<RoleResponse>
     */
    List<RoleResponse> toResponseList(List<Role> roles);

    /**
     * Update Role entity từ RoleRequest (cho cập nhật)
     * Chỉ update các field non-null từ request
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateEntityFromRequest(RoleRequest request, @MappingTarget Role role);
}