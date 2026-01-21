package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.dto.response.UserDetailResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.dto.response.UserSummaryResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.UserMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.model.composite_key.UserRoleId;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LoyaltyPointService loyaltyPointService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserResponse response = userMapper.toUserResponse(user);
        Optional<Customer> customerOpt = customerRepository.findByUser(user);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            if (customer.getTier() != null) {
                response.setTier(UserResponse.UserTierInfo.builder()
                        .name(customer.getTier().getName())
                        .discountRate(customer.getTier().getDiscountRate())
                        .minPoint(customer.getTier().getMinPoint())
                        .description(customer.getTier().getDescription())
                        .build());
            }
            if (customer.getLoyaltyPoint() != null) {
                var lp = customer.getLoyaltyPoint();
                Integer pointsToNext = loyaltyPointService.getPointsToNextTier(lp.getTotalPoints());

                response.setLoyaltyPoint(UserResponse.UserLoyaltyInfo.builder()
                        .totalPoints(lp.getTotalPoints())
                        .pointsAvailable(lp.getPointsAvailable())
                        .pointsUsed(lp.getPointsUsed())
                        .pointsToNextTier(pointsToNext)
                        .build());
            }
        }

        return response;
    }

    @Override
    public UserResponse updateProfile(UUID userId, UserRequest request, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request != null) {
            if (request.getFullName() != null) user.setFullName(request.getFullName());
            if (request.getPhone() != null) user.setPhone(request.getPhone());
        }

        // Logic Upload Avatar
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadFile(avatarFile);
            user.setAvatar(avatarUrl);
        }

        // Tắt cờ First Login
        if (Boolean.TRUE.equals(user.getIsFirstLogin())) {
            user.setIsFirstLogin(false);
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune belongs to province
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new IllegalArgumentException("Commune does not belong to the specified province");
        }

        // If this is primary, unset other primary addresses
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        // Create address
        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet(request.getStreet());
        address.setProvince(province);
        address.setCommune(commune);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());
        address.setContactName(request.getContactName());
        address.setPhoneNumber(request.getPhoneNumber());

        Address savedAddress = addressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return customer.getAddresses().stream()
                .filter(address -> !Boolean.TRUE.equals(address.getIsDeleted()))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        // Admin/Employee get addresses of any user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return customer.getAddresses().stream()
                .filter(address -> !Boolean.TRUE.equals(address.getIsDeleted()))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to this customer");
        }

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune belongs to province
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new IllegalArgumentException("Commune does not belong to the specified province");
        }

        // If this is set as primary, remove primary from other addresses
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(address.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        // Update address
        address.setStreet(request.getStreet());
        address.setProvince(province);
        address.setCommune(commune);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());

        Address updatedAddress = addressRepository.save(address);

        return mapToAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to this customer");
        }

        address.setIsDeleted(true);
        addressRepository.save(address);
    }

    private AddressResponse mapToAddressResponse(Address address) {
        String detailAddress = address.getStreet() + ", "
                + address.getCommune().getName() + ", "
                + address.getProvince().getName();

        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .commune(address.getCommune().getName())
                .communeCode(address.getCommune().getCode())
                .city(address.getProvince().getName())
                .provinceCode(address.getProvince().getCode())
                .detailAddress(detailAddress)
                .type(address.getType())
                .isPrimary(address.getIsPrimary())
                .postalCode(address.getPostalCode())
                .contactName(address.getContactName())
                .phoneNumber(address.getPhoneNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getAllUsers(Pageable pageable) {
        log.info("Getting all users with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findByIsDeletedFalse(pageable);
        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(UUID userId) {
        log.info("Getting user detail by ID: {}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return UserDetailResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers(pageable);
        }

        Page<User> userPage = userRepository.searchUsers(keyword.trim(), pageable);
        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsersByStatus(UserStatus status, Pageable pageable) {
        log.info("Getting users by status: {}", status);

        Page<User> userPage = userRepository.findByStatusAndIsDeletedFalse(status, pageable);
        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsersByRole(String roleName, Pageable pageable) {
        log.info("Getting users by role: {}", roleName);

        Page<User> userPage = userRepository.findByRoleName(roleName, pageable);
        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsersWithFilters(
            String keyword,
            UserStatus status,
            String roleName,
            Pageable pageable) {

        log.info("Searching users with filters - keyword: {}, status: {}, role: {}",
                keyword, status, roleName);

        // Normalize keyword
        String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? keyword.trim()
                : "";

        Page<User> userPage;

        // Case 1: Có cả status và role
        if (status != null && roleName != null && !roleName.isEmpty()) {
            if (normalizedKeyword.isEmpty()) {
                userPage = userRepository.findByStatusAndRoleName(status, roleName, pageable);
            } else {
                // Có keyword + status + role
                userPage = userRepository.searchUsersWithStatusAndRole(
                        normalizedKeyword, status, roleName, pageable);
            }
        }
        // Case 2: Chỉ có status
        else if (status != null) {
            if (normalizedKeyword.isEmpty()) {
                userPage = userRepository.findByStatusAndIsDeletedFalse(status, pageable);
            } else {
                userPage = userRepository.searchUsersWithStatus(normalizedKeyword, status, pageable);
            }
        }
        // Case 3: Chỉ có role
        else if (roleName != null && !roleName.isEmpty()) {
            if (normalizedKeyword.isEmpty()) {
                userPage = userRepository.findByRoleName(roleName, pageable);
            } else {
                userPage = userRepository.searchUsersWithRole(normalizedKeyword, roleName, pageable);
            }
        }
        // Case 4: Chỉ có keyword (hoặc không có gì)
        else {
            if (normalizedKeyword.isEmpty()) {
                userPage = userRepository.findByIsDeletedFalse(pageable);
            } else {
                userPage = userRepository.searchUsers(normalizedKeyword, pageable);
            }
        }

        return userPage.map(UserSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByStatus(UserStatus status) {
        return userRepository.countByStatusAndIsDeletedFalse(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(String roleName) {
        return userRepository.countByRoleName(roleName);
    }

    @Override
    @Transactional
    public UserDetailResponse updateUserStatus(UUID userId, UserStatus newStatus, String reason, UUID actorId) {
        log.info("Updating user {} status to {}", userId, newStatus);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserStatus oldStatus = user.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // Update status
        user.setStatus(newStatus);

        // Auto update isActive based on status
        if (newStatus == UserStatus.ACTIVE) {
            user.setIsActive(true);
        } else if (newStatus == UserStatus.SUSPENDED || newStatus == UserStatus.TERMINATED) {
            user.setIsActive(false);
        }

        user.setUpdatedBy(actorId);
        User updatedUser = userRepository.save(user);

        // Audit log
        auditLogService.logSuccess(actorId, AuditModule.STAFF, AuditAction.UPDATE,
                userId.toString(), "user_status",
                oldStatus.toString(), newStatus.toString());

        log.info("Updated user {} status from {} to {}", userId, oldStatus, newStatus);
        return userMapper.toUserDetailResponse(updatedUser);
    }

    /**
     * Add multiple roles at once
     */
    @Override
    @Transactional
    public UserDetailResponse addRolesToUser(UUID userId, BatchAddRolesRequest request, UUID actorId) {
        log.info("Adding multiple roles to user {}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Resolve roles from request (ưu tiên roleIds)
        Set<Role> rolesToAdd = resolveRolesFromBatchRequest(request.getRoleIds(), request.getRoleNames());

        if (rolesToAdd.isEmpty()) {
            throw new BadRequestException("No valid roles to add");
        }

        // Track added roles for logging
        List<String> addedRoleNames = new ArrayList<>();
        List<String> skippedRoleNames = new ArrayList<>();

        for (Role role : rolesToAdd) {
            if (user.getRoleNames().contains(role.getName())) {
                skippedRoleNames.add(role.getName());
                log.warn("User {} already has role {}, skipping", userId, role.getName());
                continue;
            }

            addRoleToUserInternal(user, role);
            addedRoleNames.add(role.getName());
        }

        if (addedRoleNames.isEmpty()) {
            throw new BadRequestException("User already has all specified roles: " + skippedRoleNames);
        }

        user.setUpdatedBy(actorId);
        User updatedUser = userRepository.save(user);

        // Audit log
        String auditMessage = String.format("Added roles: %s", String.join(", ", addedRoleNames));
        if (!skippedRoleNames.isEmpty()) {
            auditMessage += String.format(" | Skipped (already had): %s", String.join(", ", skippedRoleNames));
        }
        auditLogService.logSuccess(actorId, AuditModule.STAFF, AuditAction.UPDATE,
                userId.toString(), "user_roles", null, auditMessage);

        log.info("Added {} roles to user {}: {}", addedRoleNames.size(), userId, addedRoleNames);
        return userMapper.toUserDetailResponse(updatedUser);
    }

    /**
     * Remove multiple roles at once
     */
    @Override
    @Transactional
    public UserDetailResponse removeRolesFromUser(UUID userId, BatchRemoveRolesRequest request, UUID actorId) {
        log.info("Removing multiple roles from user {}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Resolve roles from request
        Set<Role> rolesToRemove = resolveRolesFromBatchRequest(request.getRoleIds(), request.getRoleNames());

        if (rolesToRemove.isEmpty()) {
            throw new BadRequestException("No valid roles to remove");
        }

        // Validate: Must keep at least 1 role
        Set<String> roleNamesToRemove = rolesToRemove.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        long remainingRoles = user.getUserRoles().stream()
                .filter(ur -> !roleNamesToRemove.contains(ur.getRole().getName()))
                .count();

        if (remainingRoles == 0) {
            throw new BadRequestException("Cannot remove all roles. User must have at least one role");
        }

        // Track removed roles for logging
        List<String> removedRoleNames = new ArrayList<>();
        List<String> notFoundRoleNames = new ArrayList<>();

        for (Role role : rolesToRemove) {
            if (!user.getRoleNames().contains(role.getName())) {
                notFoundRoleNames.add(role.getName());
                log.warn("User {} does not have role {}, skipping", userId, role.getName());
                continue;
            }

            user.getUserRoles().removeIf(ur -> ur.getRole().getName().equals(role.getName()));
            removedRoleNames.add(role.getName());
        }

        if (removedRoleNames.isEmpty()) {
            throw new BadRequestException("User does not have any of the specified roles: " + notFoundRoleNames);
        }

        user.setUpdatedBy(actorId);
        User updatedUser = userRepository.save(user);

        // Audit log
        String auditMessage = String.format("Removed roles: %s", String.join(", ", removedRoleNames));
        if (!notFoundRoleNames.isEmpty()) {
            auditMessage += String.format(" | Not found: %s", String.join(", ", notFoundRoleNames));
        }
        auditLogService.logSuccess(actorId, AuditModule.STAFF, AuditAction.UPDATE,
                userId.toString(), "user_roles", auditMessage, null);

        log.info("Removed {} roles from user {}: {}", removedRoleNames.size(), userId, removedRoleNames);
        return userMapper.toUserDetailResponse(updatedUser);
    }

    /**
     * Replace all user roles
     */
    @Override
    @Transactional
    public UserDetailResponse replaceUserRoles(UUID userId, ReplaceUserRolesRequest request, UUID actorId) {
        log.info("Replacing all roles for user {}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Resolve new roles from request
        Set<Role> newRoles = resolveRolesFromBatchRequest(request.getRoleIds(), request.getRoleNames());

        if (newRoles.isEmpty()) {
            throw new BadRequestException("At least one new role must be provided");
        }

        // Get old roles for logging
        Set<String> oldRoleNames = new HashSet<>(user.getRoleNames());

        // Clear all existing roles
        user.getUserRoles().clear();

        // Add new roles
        for (Role role : newRoles) {
            addRoleToUserInternal(user, role);
        }

        user.setUpdatedBy(actorId);
        User updatedUser = userRepository.save(user);

        // Audit log
        Set<String> newRoleNames = newRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String auditMessage = String.format("Replaced roles | Old: %s | New: %s",
                String.join(", ", oldRoleNames),
                String.join(", ", newRoleNames));

        auditLogService.logSuccess(actorId, AuditModule.STAFF, AuditAction.UPDATE,
                userId.toString(), "user_roles", String.join(", ", oldRoleNames),
                String.join(", ", newRoleNames));

        log.info("Replaced roles for user {}: {} -> {}", userId, oldRoleNames, newRoleNames);
        return userMapper.toUserDetailResponse(updatedUser);
    }

// ========================================
// HELPER METHODS (PRIVATE)
// ========================================

    /**
     * Internal method để add role vào user (không save DB)
     */
    private void addRoleToUserInternal(User user, Role role) {
        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());
        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);
    }

    /**
     * Resolve roles từ roleIds hoặc roleNames (ưu tiên roleIds)
     */
    private Set<Role> resolveRolesFromBatchRequest(Set<UUID> roleIds, Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();

        // Ưu tiên roleIds
        if (roleIds != null && !roleIds.isEmpty()) {
            for (UUID roleId : roleIds) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }
            return roles;
        }

        // Fallback to roleNames
        if (roleNames != null && !roleNames.isEmpty()) {
            for (String roleName : roleNames) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
        }

        return roles;
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Validate status transition rules
     */
    private void validateStatusTransition(UserStatus oldStatus, UserStatus newStatus) {
        // PENDING can only go to ACTIVE
        if (oldStatus == UserStatus.PENDING && newStatus != UserStatus.ACTIVE) {
            throw new BadRequestException("PENDING users can only be activated to ACTIVE");
        }

        // TERMINATED is final state
        if (oldStatus == UserStatus.TERMINATED) {
            throw new BadRequestException("Cannot change status of TERMINATED users");
        }

        // Cannot go back to PENDING
        if (newStatus == UserStatus.PENDING) {
            throw new BadRequestException("Cannot set user status to PENDING");
        }
    }
}