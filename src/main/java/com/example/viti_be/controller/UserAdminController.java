package com.example.viti_be.controller;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.UserDetailResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller quản lý Users (Admin only)
 * Endpoints: /api/admin/users
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management (Admin)", description = "APIs for managing users - Admin only")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserService userService;

    /**
     * Lấy danh sách tất cả users với pagination
     * GET /api/admin/users
     */
    @GetMapping
    @Operation(summary = "Get all users with pagination",
            description = "Returns paginated list of users. Default sort by createdAt DESC")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getAllUsers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy danh sách users thành công"));
    }

    /**
     * Lấy chi tiết user theo ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user detail by ID",
            description = "Returns detailed user information including roles and metadata")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable UUID id) {
        UserDetailResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "Lấy thông tin user thành công"));
    }

    /**
     * Search users theo keyword
     * GET /api/admin/users/search?keyword=...
     */
    @GetMapping("/search")
    @Operation(summary = "Search users by keyword",
            description = "Search users by username, email, fullName, or phone")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> searchUsers(
            @Parameter(description = "Search keyword (username, email, fullName, phone)")
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Tìm kiếm users thành công"));
    }

    /**
     * Lấy users theo status
     * GET /api/admin/users/by-status?status=ACTIVE
     */
    @GetMapping("/by-status")
    @Operation(summary = "Get users by status",
            description = "Filter users by status (ACTIVE, INACTIVE, LOCKED)")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getUsersByStatus(
            @Parameter(description = "User status (ACTIVE, INACTIVE, LOCKED)")
            @RequestParam UserStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = userService.getUsersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users,
                String.format("Lấy users theo status %s thành công", status)));
    }

    /**
     * Lấy users theo role
     * GET /api/admin/users/by-role?roleName=ROLE_ADMIN
     */
    @GetMapping("/by-role")
    @Operation(summary = "Get users by role",
            description = "Filter users by role (ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER)")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getUsersByRole(
            @Parameter(description = "Role name (ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER)")
            @RequestParam String roleName,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = userService.getUsersByRole(roleName, pageable);
        return ResponseEntity.ok(ApiResponse.success(users,
                String.format("Lấy users theo role %s thành công", roleName)));
    }

    /**
     * Search users với filters (status và/hoặc role)
     * GET /api/admin/users/filter?keyword=...&status=ACTIVE&roleName=ROLE_CUSTOMER
     */
    @GetMapping("/filter")
    @Operation(summary = "Search users with filters",
            description = "Advanced search with keyword, status, and role filters. All params are optional")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> searchUsersWithFilters(
            @Parameter(description = "Search keyword (optional)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Filter by role (optional)")
            @RequestParam(required = false) String roleName,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserSummaryResponse> users = userService.searchUsersWithFilters(
                keyword, status, roleName, pageable);

        return ResponseEntity.ok(ApiResponse.success(users, "Tìm kiếm users với filters thành công"));
    }

    /**
     * Thống kê users
     * GET /api/admin/users/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get user statistics",
            description = "Returns counts by status and role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count by status
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("ACTIVE", userService.countUsersByStatus(UserStatus.ACTIVE));
        statusCounts.put("PENDING", userService.countUsersByStatus(UserStatus.PENDING));
        statusCounts.put("TERMINATED", userService.countUsersByStatus(UserStatus.TERMINATED));
        statusCounts.put("SUSPENDED", userService.countUsersByStatus(UserStatus.SUSPENDED));

        stats.put("byStatus", statusCounts);

        // Count by role
        Map<String, Long> roleCounts = new HashMap<>();
        roleCounts.put("ROLE_ADMIN", userService.countUsersByRole("ROLE_ADMIN"));
        roleCounts.put("ROLE_EMPLOYEE", userService.countUsersByRole("ROLE_EMPLOYEE"));
        roleCounts.put("ROLE_CUSTOMER", userService.countUsersByRole("ROLE_CUSTOMER"));
        stats.put("byRole", roleCounts);

        // Total
        stats.put("total", statusCounts.values().stream().mapToLong(Long::longValue).sum());

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê users thành công"));
    }

    /**
     * Update user status
     * PUT /api/admin/users/{id}/status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status",
            description = "Change user status to ACTIVE, SUSPENDED, or TERMINATED")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.updateUserStatus(
                id, request.getNewStatus(), request.getReason(), actorId);

        return ResponseEntity.ok(ApiResponse.success(user,
                String.format("Cập nhật status user thành %s thành công", request.getNewStatus())));
    }

    /**
     * Activate user (shortcut)
     * POST /api/admin/users/{id}/activate
     */
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Set user status to ACTIVE")
    public ResponseEntity<ApiResponse<UserDetailResponse>> activateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.updateUserStatus(
                id, UserStatus.ACTIVE, "Activated by admin", actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Kích hoạt user thành công"));
    }

    /**
     * Suspend user (shortcut)
     * POST /api/admin/users/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend user", description = "Set user status to SUSPENDED")
    public ResponseEntity<ApiResponse<UserDetailResponse>> suspendUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.updateUserStatus(
                id, UserStatus.SUSPENDED, reason != null ? reason : "Suspended by admin", actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Tạm ngưng user thành công"));
    }

    /**
     * Terminate user (shortcut)
     * POST /api/admin/users/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate user",
            description = "Set user status to TERMINATED (final state, cannot be changed)")
    public ResponseEntity<ApiResponse<UserDetailResponse>> terminateUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.updateUserStatus(
                id, UserStatus.TERMINATED, reason != null ? reason : "Terminated by admin", actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Chấm dứt user thành công"));
    }

    // ========================================
    // ROLE MANAGEMENT
    // ========================================

    /**
     * Add multiple roles to user at once
     * POST /api/admin/users/{id}/roles/batch-add
     */
    @PostMapping("/{id}/roles/batch-add")
    @Operation(summary = "Add multiple roles to user",
            description = "Add multiple roles to user at once. Provide either roleNames or roleIds. Skips roles user already has.")
    public ResponseEntity<ApiResponse<UserDetailResponse>> addRolesToUser(
            @PathVariable UUID id,
            @Valid @RequestBody BatchAddRolesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.addRolesToUser(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Thêm các roles cho user thành công"));
    }

    /**
     * Remove multiple roles from user at once
     * POST /api/admin/users/{id}/roles/batch-remove
     */
    @PostMapping("/{id}/roles/batch-remove")
    @Operation(summary = "Remove multiple roles from user",
            description = "Remove multiple roles from user at once. User must have at least one role remaining.")
    public ResponseEntity<ApiResponse<UserDetailResponse>> removeRolesFromUser(
            @PathVariable UUID id,
            @Valid @RequestBody BatchRemoveRolesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.removeRolesFromUser(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Xóa các roles khỏi user thành công"));
    }

    /**
     * Replace all user roles with new ones
     * PUT /api/admin/users/{id}/roles/replace
     */
    @PutMapping("/{id}/roles/replace")
    @Operation(summary = "Replace all user roles",
            description = "Remove all existing roles and assign new ones. Useful for changing user from Employee to Admin.")
    public ResponseEntity<ApiResponse<UserDetailResponse>> replaceUserRoles(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceUserRolesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        UserDetailResponse user = userService.replaceUserRoles(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(user, "Thay thế roles của user thành công"));
    }
}