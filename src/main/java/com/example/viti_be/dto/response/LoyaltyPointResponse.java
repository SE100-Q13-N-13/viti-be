package com.example.viti_be.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private Integer totalPoints;
    private Integer pointsAvailable;
    private Integer pointsUsed;
    private BigDecimal pointRate;
    private LocalDateTime lastEarnedAt;
    private LocalDateTime lastUsedAt;
    private String currentTier;
    private Integer pointsToNextTier;
    private BigDecimal availablePointsValue; // points_available * point_rate
}
