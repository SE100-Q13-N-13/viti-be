package com.example.viti_be.service;

import com.example.viti_be.dto.request.RoleRequest;
import com.example.viti_be.dto.response.RoleResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    /**
     * Tạo role mới
     * @param request RoleRequest chứa thông tin role
     * @param actorId UUID của user thực hiện (cho audit)
     * @return RoleResponse
     */
    RoleResponse createRole(RoleRequest request, UUID actorId);

    /**
     * Cập nhật role
     * @param roleId UUID của role cần update
     * @param request RoleRequest chứa thông tin mới
     * @param actorId UUID của user thực hiện (cho audit)
     * @return RoleResponse
     */
    RoleResponse updateRole(UUID roleId, RoleRequest request, UUID actorId);

    /**
     * Xóa role (soft delete)
     * @param roleId UUID của role cần xóa
     * @param actorId UUID của user thực hiện (cho audit)
     */
    void deleteRole(UUID roleId, UUID actorId);

    /**
     * Lấy tất cả roles (có pagination)
     * @param pageable Pagination info
     * @return Page<RoleResponse>
     */
    Page<RoleResponse> getAllRoles(Pageable pageable);

    /**
     * Lấy tất cả roles (không pagination) - cho dropdown
     * @return List<RoleResponse>
     */
    List<RoleResponse> getAllRolesNoPaging();

    /**
     * Lấy role theo ID
     * @param roleId UUID của role
     * @return RoleResponse
     */
    RoleResponse getRoleById(UUID roleId);

    /**
     * Lấy role theo name
     * @param name Tên role (VD: ROLE_ADMIN)
     * @return RoleResponse
     */
    RoleResponse getRoleByName(String name);

    /**
     * Kiểm tra role có tồn tại không
     * @param name Tên role
     * @return true nếu tồn tại
     */
    boolean existsByName(String name);

    /**
     * Lấy danh sách users có role này
     * @param roleId UUID của role
     * @param pageable Pagination info
     * @return Page<UserSummaryResponse>
     */
    Page<UserSummaryResponse> getUsersByRole(UUID roleId, Pageable pageable);

    /**
     * Đếm số users có role này
     * @param roleId UUID của role
     * @return số lượng users
     */
    long countUsersByRole(UUID roleId);
}