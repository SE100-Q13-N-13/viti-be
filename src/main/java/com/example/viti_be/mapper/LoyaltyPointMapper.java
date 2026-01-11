package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.LoyaltyPointResponse;
import com.example.viti_be.dto.response.LoyaltyPointTransactionResponse;
import com.example.viti_be.model.LoyaltyPoint;
import com.example.viti_be.model.LoyaltyPointTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoyaltyPointMapper {

    /**
     * Convert LoyaltyPoint entity to Response DTO
     */
    public LoyaltyPointResponse toResponse(LoyaltyPoint loyaltyPoint, String currentTier, Integer pointsToNextTier) {
        if (loyaltyPoint == null) {
            return null;
        }

        BigDecimal availableValue = loyaltyPoint.getPointRate()
                .multiply(BigDecimal.valueOf(loyaltyPoint.getPointsAvailable()));

        return LoyaltyPointResponse.builder()
                .id(loyaltyPoint.getId())
                .customerId(loyaltyPoint.getCustomer().getId())
                .customerName(loyaltyPoint.getCustomer().getFullName())
                .totalPoints(loyaltyPoint.getTotalPoints())
                .pointsAvailable(loyaltyPoint.getPointsAvailable())
                .pointsUsed(loyaltyPoint.getPointsUsed())
                .pointRate(loyaltyPoint.getPointRate())
                .lastEarnedAt(loyaltyPoint.getLastEarnedAt())
                .lastUsedAt(loyaltyPoint.getLastUsedAt())
                .currentTier(currentTier)
                .pointsToNextTier(pointsToNextTier)
                .availablePointsValue(availableValue)
                .build();
    }

    /**
     * Convert Transaction entity to Response DTO
     */
    public LoyaltyPointTransactionResponse toTransactionResponse(LoyaltyPointTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return LoyaltyPointTransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .pointsChange(transaction.getPointsChange())
                .pointsTotalAfter(transaction.getPointsTotalAfter())
                .pointsAvailableAfter(transaction.getPointsAvailableAfter())
                .reason(transaction.getReason())
                .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
                .orderNumber(transaction.getOrder() != null ? transaction.getOrder().getId().toString() : null)
                .performedByName(transaction.getPerformedBy() != null ?
                        transaction.getPerformedBy().getFullName() : "System")
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}