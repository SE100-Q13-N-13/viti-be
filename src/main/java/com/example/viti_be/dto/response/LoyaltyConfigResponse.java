package com.example.viti_be.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyConfigResponse {
    // Earning
    private Boolean earnEnabled;
    private Integer earnRate;
    private Integer minOrderToEarn;

    // Redemption
    private Boolean redeemEnabled;
    private Integer redeemRate;
    private Integer maxRedeemPercent;
    private Integer minOrderToRedeem;

    // Tiers
    private Integer regularMinPoints;
    private Integer loyalMinPoints;
    private Integer goldMinPoints;
    private Integer platinumMinPoints;

    // Reset
    private Boolean resetEnabled;
    private Integer resetPeriodMonths;
    private LocalDateTime nextResetDate;
}