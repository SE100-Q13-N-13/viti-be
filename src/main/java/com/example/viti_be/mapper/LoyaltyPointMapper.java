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
    @Mapping(target = "customerName", source = "loyaltyPoint.customer.fullName") // Giả sử User có field fullName
    @Mapping(target = "currentTier", source = "currentTier") // Map từ tham số truyền vào
    @Mapping(target = "pointsToNextTier", source = "pointsToNextTier") // Map từ tham số truyền vào
    @Mapping(target = "availablePointsValue", source = "loyaltyPoint", qualifiedByName = "calculateAvailableValue")
    LoyaltyPointResponse toResponse(LoyaltyPoint loyaltyPoint, String currentTier, Integer pointsToNextTier);

    // --- MAPPING TRANSACTION ---

    @Mapping(target = "orderId", source = "order.id")
    // Map order.id sang String orderNumber (như logic cũ)
    @Mapping(target = "orderNumber", source = "order.id")
    // Nếu performedBy null, mặc định là "System"
    @Mapping(target = "performedByName", source = "performedBy.fullName", defaultValue = "System")
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