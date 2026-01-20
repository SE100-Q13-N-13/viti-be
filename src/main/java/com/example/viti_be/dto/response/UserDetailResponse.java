package com.example.viti_be.dto.response;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Detailed response cho get user by ID (Admin view)
 * Bao gồm thêm metadata và verification info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private UserStatus status;
    private Boolean isActive;
    private Boolean isFirstLogin;
    private Set<String> roles;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    // Verification info (optional, cho admin debug)
    private LocalDateTime verificationExpiration;
    private Instant tokenExpiryDate;

    /**
     * Map từ User entity
     */
    public static UserDetailResponse fromEntity(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .isFirstLogin(user.getIsFirstLogin())
                .roles(user.getRoleNames())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .verificationExpiration(user.getVerificationExpiration())
                .tokenExpiryDate(user.getTokenExpiryDate())
                .build();
    }
}