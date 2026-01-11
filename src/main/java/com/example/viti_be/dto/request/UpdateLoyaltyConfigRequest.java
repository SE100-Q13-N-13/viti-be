package com.example.viti_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLoyaltyConfigRequest {

    // Earning
    private Boolean earnEnabled;

    @Positive(message = "Earn rate must be positive")
    private Integer earnRate;

    @PositiveOrZero(message = "Min order amount must be non-negative")
    private Integer minOrderToEarn;

    // Redemption
    private Boolean redeemEnabled;

    @Positive(message = "Redeem rate must be positive")
    private Integer redeemRate;

    @Min(value = 0, message = "Max redeem percent must be between 0 and 100")
    @Max(value = 100, message = "Max redeem percent must be between 0 and 100")
    private Integer maxRedeemPercent;

    @PositiveOrZero(message = "Min order to redeem must be non-negative")
    private Integer minOrderToRedeem;

    // Tiers
    private Integer regularMinPoints;
    private Integer loyalMinPoints;
    private Integer goldMinPoints;
    private Integer platinumMinPoints;

    // Reset
    private Boolean resetEnabled;

    @Positive(message = "Reset period must be positive")
    private Integer resetPeriodMonths;
}
