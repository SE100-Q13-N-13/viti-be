package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.UserDetailResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.model.User;
import com.example.viti_be.security.services.UserDetailsImpl;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct Mapper cho User entities
 * Generates implementation automatically at compile time
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserDetailsImpl toUserDetails(User user) {
        if (user == null) {
            return null;
        }
        return UserDetailsImpl.build(user);
    }

    /**
     * Map User entity -> UserResponse (cho current user profile)
     */
    @Mapping(target = "isActive", expression = "java(user.getIsActive())")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "tier", ignore = true)
    @Mapping(target = "loyaltyPoint", ignore = true)
    UserResponse toUserResponse(User user);

    /**
     * Map User entity -> UserSummaryResponse (cho admin list view)
     */
    @Mapping(target = "roles", expression = "java(user.getRoleNames())")
    UserSummaryResponse toUserSummaryResponse(User user);

    /**
     * Map User entity -> UserDetailResponse (cho admin detail view)
     */
    @Mapping(target = "roles", expression = "java(user.getRoleNames())")
    UserDetailResponse toUserDetailResponse(User user);

    /**
     * Map list of User entities -> list of UserSummaryResponse
     */
    List<UserSummaryResponse> toUserSummaryResponseList(List<User> users);

    /**
     * After mapping for UserResponse - set roles manually
     */
    @AfterMapping
    default void setRoles(@MappingTarget UserResponse response, User user) {
        if (user != null && user.getUserRoles() != null) {
            response.setRoles(user.getRoleNames());
        }
    }
}