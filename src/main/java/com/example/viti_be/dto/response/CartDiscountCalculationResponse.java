package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDiscountCalculationResponse {
    private BigDecimal subtotal;
    private BigDecimal tierDiscount;
    private BigDecimal productPromotionDiscount;
    private BigDecimal orderPromotionDiscount;
    private BigDecimal totalDiscount;
    private BigDecimal finalAmount;

    private List<AppliedPromotionResponse> productPromotions;
    private AppliedPromotionResponse orderPromotion;
    private List<String> warnings; // "Promotion X không áp dụng được vì..."
}
