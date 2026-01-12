package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageDetailResponse {
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt;
}
