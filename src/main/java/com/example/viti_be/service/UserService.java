package com.example.viti_be.service;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.dto.response.UserDetailResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.model.model_enum.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse getUserProfile(UUID userId);
    UserResponse updateProfile(UUID userId, UserRequest request, MultipartFile avatarFile);

    // Address management methods
    AddressResponse addAddress(UUID userId, AddressRequest request);
    List<AddressResponse> getAddresses(UUID userId);
    List<AddressResponse> getAddressesByUserId(UUID userId);
    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID userId, UUID addressId);

    /**
     * Lấy tất cả users với pagination (Admin only)
     * @param pageable Pagination params (page, size, sort)
     * @return Page of UserSummaryResponse
     */
    Page<UserSummaryResponse> getAllUsers(Pageable pageable);

    /**
     * Lấy chi tiết user theo ID (Admin only)
     * @param userId User ID
     * @return UserDetailResponse với đầy đủ thông tin
     */
    UserDetailResponse getUserById(UUID userId);

    /**
     * Search users theo keyword với pagination
     * @param keyword Tìm theo username, email, fullName, phone
     * @param pageable Pagination params
     * @return Page of UserSummaryResponse
     */
    Page<UserSummaryResponse> searchUsers(String keyword, Pageable pageable);

    /**
     * Lấy users theo status với pagination
     * @param status UserStatus (ACTIVE, INACTIVE, LOCKED)
     * @param pageable Pagination params
     * @return Page of UserSummaryResponse
     */
    Page<UserSummaryResponse> getUsersByStatus(UserStatus status, Pageable pageable);

    /**
     * Lấy users theo role với pagination
     * @param roleName Role name (ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER)
     * @param pageable Pagination params
     * @return Page of UserSummaryResponse
     */
    Page<UserSummaryResponse> getUsersByRole(String roleName, Pageable pageable);

    /**
     * Search users với filter (status và/hoặc role)
     * @param keyword Search keyword (có thể null)
     * @param status Filter by status (có thể null)
     * @param roleName Filter by role (có thể null)
     * @param pageable Pagination params
     * @return Page of UserSummaryResponse
     */
    Page<UserSummaryResponse> searchUsersWithFilters(
            String keyword,
            UserStatus status,
            String roleName,
            Pageable pageable
    );

    /**
     * Đếm users theo status
     * @param status UserStatus
     * @return Count
     */
    long countUsersByStatus(UserStatus status);

    /**
     * Đếm users theo role
     * @param roleName Role name
     * @return Count
     */
    long countUsersByRole(String roleName);

    /**
     * Update user status (ACTIVE, SUSPENDED, TERMINATED)
     * @param userId User ID
     * @param newStatus New status
     * @param reason Reason for change
     * @param actorId Admin performing the action
     * @return Updated UserDetailResponse
     */
    UserDetailResponse updateUserStatus(UUID userId, UserStatus newStatus, String reason, UUID actorId);

    /**
     * Add multiple roles to user at once
     * @param userId UUID của user
     * @param request BatchAddRolesRequest (chứa roleNames hoặc roleIds)
     * @param actorId UUID của user thực hiện
     * @return UserDetailResponse
     */
    UserDetailResponse addRolesToUser(UUID userId, BatchAddRolesRequest request, UUID actorId);

    /**
     * Remove multiple roles from user at once
     * @param userId UUID của user
     * @param request BatchRemoveRolesRequest (chứa roleNames hoặc roleIds)
     * @param actorId UUID của user thực hiện
     * @return UserDetailResponse
     */
    UserDetailResponse removeRolesFromUser(UUID userId, BatchRemoveRolesRequest request, UUID actorId);

    /**
     * Replace all user roles with new ones
     * Xóa tất cả roles hiện tại và set roles mới
     * @param userId UUID của user
     * @param request ReplaceUserRolesRequest (chứa roleNames hoặc roleIds)
     * @param actorId UUID của user thực hiện
     * @return UserDetailResponse
     */
    UserDetailResponse replaceUserRoles(UUID userId, ReplaceUserRolesRequest request, UUID actorId);
}
