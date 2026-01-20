package com.example.viti_be.repository;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<User> findByIdAndIsDeletedFalse(UUID id);
    List<User> findByStatusAndCreatedAtBefore(UserStatus status, LocalDateTime createdAt);

    /**
     * Tìm tất cả users chưa bị xóa (với pagination)
     */
    Page<User> findByIsDeletedFalse(Pageable pageable);

    /**
     * Tìm users theo status (với pagination)
     */
    Page<User> findByStatusAndIsDeletedFalse(UserStatus status, Pageable pageable);

    /**
     * Tìm users theo role name (với pagination)
     * Query phức tạp vì Role nằm trong UserRole (many-to-many)
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "WHERE r.name = :roleName AND u.isDeleted = false")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    /**
     * Search users theo keyword (username, email, fullName, phone)
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Search users với filter theo status
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.status = :status AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsersWithStatus(@Param("keyword") String keyword,
                                     @Param("status") UserStatus status,
                                     Pageable pageable);

    /**
     * Search users với filter theo role
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "WHERE u.isDeleted = false AND r.name = :roleName AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsersWithRole(@Param("keyword") String keyword,
                                   @Param("roleName") String roleName,
                                   Pageable pageable);

    /**
     * Search users với filter theo status VÀ role
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "WHERE u.isDeleted = false AND u.status = :status AND r.name = :roleName AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsersWithStatusAndRole(@Param("keyword") String keyword,
                                            @Param("status") UserStatus status,
                                            @Param("roleName") String roleName,
                                            Pageable pageable);

    /**
     * Count users by status
     */
    long countByStatusAndIsDeletedFalse(UserStatus status);

    /**
     * Count users by role
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "WHERE r.name = :roleName AND u.isDeleted = false")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * Tìm users theo isActive (với pagination)
     */
    Page<User> findByIsActiveAndIsDeletedFalse(Boolean isActive, Pageable pageable);

    /**
     * Filter by status AND role (without keyword)
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "WHERE u.isDeleted = false AND u.status = :status AND r.name = :roleName")
    Page<User> findByStatusAndRoleName(@Param("status") UserStatus status,
                                       @Param("roleName") String roleName,
                                       Pageable pageable);
}