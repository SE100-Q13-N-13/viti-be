package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.LoyaltyPointResponse;
import com.example.viti_be.dto.response.LoyaltyPointTransactionResponse;
import com.example.viti_be.model.LoyaltyPoint;
import com.example.viti_be.model.LoyaltyPointTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface LoyaltyPointMapper {

    // --- MAPPING LOYALTY POINT ---

    @Mapping(target = "customerId", source = "loyaltyPoint.customer.id")
    @Mapping(target = "customerName", expression = "java(loyaltyPoint.getCustomer() != null ? loyaltyPoint.getCustomer().getFullName() : null)")
    @Mapping(target = "currentTier", source = "currentTier")
    @Mapping(target = "pointsToNextTier", source = "pointsToNextTier")
    @Mapping(target = "availablePointsValue", source = "loyaltyPoint", qualifiedByName = "calculateAvailableValue")
    LoyaltyPointResponse toResponse(LoyaltyPoint loyaltyPoint, String currentTier, Integer pointsToNextTier);

    // --- MAPPING TRANSACTION ---

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber") // Order có field orderNumber
    // SỬA: Dùng expression tương tự cho an toàn
    @Mapping(target = "performedByName", expression = "java(transaction.getPerformedBy() != null ? transaction.getPerformedBy().getFullName() : \"System\")")
    LoyaltyPointTransactionResponse toTransactionResponse(LoyaltyPointTransaction transaction);

    // --- HELPER METHODS ---

    @Named("calculateAvailableValue")
    default BigDecimal calculateAvailableValue(LoyaltyPoint loyaltyPoint) {
        if (loyaltyPoint == null || loyaltyPoint.getPointsAvailable() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = loyaltyPoint.getPointRate() != null ? loyaltyPoint.getPointRate() : BigDecimal.ZERO;
        return rate.multiply(BigDecimal.valueOf(loyaltyPoint.getPointsAvailable()));
    }
}