package com.example.viti_be.controller;

import com.example.viti_be.dto.request.RoleRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.RoleResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý Roles (Admin only)
 * Endpoints: /api/admin/roles
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management (Admin)", description = "APIs for managing roles - Admin only")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    /**
     * Tạo role mới
     * POST /api/admin/roles
     */
    @PostMapping
    @Operation(summary = "Create new role",
            description = "Create a new role. Role name must be unique and follow format ROLE_*")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        RoleResponse response = roleService.createRole(request, actorId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo role thành công"));
    }

    /**
     * Lấy tất cả roles (có pagination)
     * GET /api/admin/roles
     */
    @GetMapping
    @Operation(summary = "Get all roles with pagination",
            description = "Returns paginated list of roles with user count")
    public ResponseEntity<ApiResponse<Page<RoleResponse>>> getAllRoles(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<RoleResponse> roles = roleService.getAllRoles(pageable);
        return ResponseEntity.ok(ApiResponse.success(roles, "Lấy danh sách roles thành công"));
    }

    /**
     * Lấy tất cả roles (không pagination) - cho dropdown
     * GET /api/admin/roles/all
     */
    @GetMapping("/all")
    @Operation(summary = "Get all roles without pagination",
            description = "Returns all roles for dropdown/select components")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRolesNoPaging() {
        List<RoleResponse> roles = roleService.getAllRolesNoPaging();
        return ResponseEntity.ok(ApiResponse.success(roles, "Lấy danh sách roles thành công"));
    }

    /**
     * Lấy chi tiết role theo ID
     * GET /api/admin/roles/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID",
            description = "Returns detailed role information including user count")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID id) {
        RoleResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(role, "Lấy thông tin role thành công"));
    }

    /**
     * Lấy role theo name
     * GET /api/admin/roles/by-name?name=ROLE_ADMIN
     */
    @GetMapping("/by-name")
    @Operation(summary = "Get role by name",
            description = "Returns role information by name (e.g., ROLE_ADMIN)")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleByName(@RequestParam String name) {
        RoleResponse role = roleService.getRoleByName(name);
        return ResponseEntity.ok(ApiResponse.success(role, "Lấy thông tin role thành công"));
    }

    /**
     * Cập nhật role
     * PUT /api/admin/roles/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update role",
            description = "Update role name and/or description. Cannot update if name conflicts with existing role.")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        RoleResponse response = roleService.updateRole(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật role thành công"));
    }

    /**
     * Xóa role
     * DELETE /api/admin/roles/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role",
            description = "Soft delete role. Cannot delete if users are still assigned to this role.")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        roleService.deleteRole(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa role thành công"));
    }

    /**
     * Lấy danh sách users có role này
     * GET /api/admin/roles/{id}/users
     */
    @GetMapping("/{id}/users")
    @Operation(summary = "Get users by role",
            description = "Returns paginated list of users assigned to this role")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getUsersByRole(
            @PathVariable UUID id,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = roleService.getUsersByRole(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(users,
                "Lấy danh sách users theo role thành công"));
    }

    /**
     * Kiểm tra role name có tồn tại không
     * GET /api/admin/roles/exists?name=ROLE_ADMIN
     */
    @GetMapping("/exists")
    @Operation(summary = "Check if role name exists",
            description = "Returns true if role name already exists")
    public ResponseEntity<ApiResponse<Boolean>> existsByName(@RequestParam String name) {
        boolean exists = roleService.existsByName(name);
        return ResponseEntity.ok(ApiResponse.success(exists,
                exists ? "Role name đã tồn tại" : "Role name chưa tồn tại"));
    }
}