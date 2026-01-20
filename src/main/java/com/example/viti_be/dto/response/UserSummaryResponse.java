package com.example.viti_be.dto.response;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Summary response cho list users (Admin view)
 * Bao gồm roles để dễ filter/display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private UserStatus status;
    private Boolean isActive;
    private Set<String> roles;  // ["ROLE_ADMIN", "ROLE_EMPLOYEE"]
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Map từ User entity
     */
    public static UserSummaryResponse fromEntity(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .roles(user.getRoleNames())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}