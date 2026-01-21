package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.RoleRequest;
import com.example.viti_be.dto.response.RoleResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.RoleMapper;
import com.example.viti_be.model.Role;
import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.repository.RoleRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request, UUID actorId) {
        log.info("Creating new role: {}", request.getName());

        // Kiểm tra role name đã tồn tại chưa
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Role name already exists: " + request.getName());
        }

        // Tạo role mới
        Role role = roleMapper.toEntity(request);
        role.setCreatedBy(actorId);
        role.setUpdatedBy(actorId);

        Role savedRole = roleRepository.save(role);

        // Audit log
        auditLogService.logSuccess(
                actorId,
                AuditModule.STAFF,
                AuditAction.CREATE,
                savedRole.getId().toString(),
                "role",
                null,
                "Created role: " + savedRole.getName()
        );

        log.info("Created role successfully: {} with ID: {}", savedRole.getName(), savedRole.getId());

        RoleResponse response = roleMapper.toResponse(savedRole);
        response.setUserCount(0L);
        return response;
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID roleId, RoleRequest request, UUID actorId) {
        log.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        // Nếu đổi tên role, kiểm tra tên mới có trùng không
        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Role name already exists: " + request.getName());
            }
        }

        String oldValue = "name: " + role.getName() + ", description: " + role.getDescription();

        // Update role
        roleMapper.updateEntityFromRequest(request, role);
        role.setUpdatedBy(actorId);

        Role updatedRole = roleRepository.save(role);

        String newValue = "name: " + updatedRole.getName() + ", description: " + updatedRole.getDescription();

        // Audit log
        auditLogService.logSuccess(
                actorId,
                AuditModule.STAFF,
                AuditAction.UPDATE,
                updatedRole.getId().toString(),
                "role",
                oldValue,
                newValue
        );

        log.info("Updated role successfully: {}", updatedRole.getId());

        RoleResponse response = roleMapper.toResponse(updatedRole);
        response.setUserCount(countUsersByRole(roleId));
        return response;
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId, UUID actorId) {
        log.info("Deleting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        // Kiểm tra xem còn users đang dùng role này không
        long userCount = userRepository.countByRoleName(role.getName());
        if (userCount > 0) {
            throw new BadRequestException(
                    String.format("Cannot delete role '%s' because %d user(s) are still using it",
                            role.getName(), userCount)
            );
        }

        // Soft delete
        role.setIsDeleted(true);
        role.setUpdatedBy(actorId);
        roleRepository.save(role);

        // Audit log
        auditLogService.logSuccess(
                actorId,
                AuditModule.STAFF,
                AuditAction.DELETE,
                role.getId().toString(),
                "role",
                "Deleted role: " + role.getName(),
                null
        );

        log.info("Deleted role successfully: {}", roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllRoles(Pageable pageable) {
        log.info("Getting all roles with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Role> rolePage = roleRepository.findAll(pageable);

        return rolePage.map(role -> {
            RoleResponse response = roleMapper.toResponse(role);
            response.setUserCount(userRepository.countByRoleName(role.getName()));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRolesNoPaging() {
        log.info("Getting all roles without pagination");

        List<Role> roles = roleRepository.findAll();

        return roles.stream()
                .map(role -> {
                    RoleResponse response = roleMapper.toResponse(role);
                    response.setUserCount(userRepository.countByRoleName(role.getName()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        log.info("Getting role by ID: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        RoleResponse response = roleMapper.toResponse(role);
        response.setUserCount(userRepository.countByRoleName(role.getName()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String name) {
        log.info("Getting role by name: {}", name);

        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));

        RoleResponse response = roleMapper.toResponse(role);
        response.setUserCount(userRepository.countByRoleName(role.getName()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roleRepository.findByName(name).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsersByRole(UUID roleId, Pageable pageable) {
        log.info("Getting users by role ID: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        Page<User> userPage = userRepository.findByRoleName(role.getName(), pageable);
        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        return userRepository.countByRoleName(role.getName());
    }
}