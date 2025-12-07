package com.example.viti_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private BigDecimal totalPurchase;
    private CustomerTierResponse tier;
    private LoyaltyPointResponse loyaltyPoint;
    private List<AddressResponse> addresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerTierResponse {
        private UUID id;
        private String name;
        private Integer minPoint;
        private BigDecimal discountRate;
        private String description;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoyaltyPointResponse {
        private UUID id;
        private Integer totalPoints;
        private Integer pointsAvailable;
        private Integer pointsUsed;
        private BigDecimal pointRate;
        private LocalDateTime lastEarnedAt;
        private LocalDateTime lastUsedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private UUID id;
        private String street;
        private String commune;
        private String city;
        private String detailAddress;
        private String type;
        private Boolean isPrimary;
        private String postalCode;
    }
}
