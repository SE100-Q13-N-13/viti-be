package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.PromotionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedPromotionResponse {
    private UUID promotionId;
    private String code;
    private String name;
    private PromotionType type;
    private BigDecimal discountAmount;
    private String message; // "Giảm 10% (tối đa 1 triệu)"
}
