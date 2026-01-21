package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private UserStatus status;
    private Boolean isFirstLogin;
    private Boolean isActive;
    private Set<String> roles;

    private UserTierInfo tier;
    private UserLoyaltyInfo loyaltyPoint;

    @Data
    @Builder
    public static class UserTierInfo {
        private String name;
        private BigDecimal discountRate;
        private Integer minPoint;
        private String description;
    }

    @Data
    @Builder
    public static class UserLoyaltyInfo {
        private Integer totalPoints;
        private Integer pointsAvailable;
        private Integer pointsUsed;
        private Integer pointsToNextTier;
    }
}