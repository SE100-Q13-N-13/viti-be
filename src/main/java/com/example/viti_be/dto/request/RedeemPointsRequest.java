package com.example.viti_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemPointsRequest {

    @NotNull(message = "Points to redeem is required")
    @Positive(message = "Points must be positive")
    private Integer pointsToRedeem;
}