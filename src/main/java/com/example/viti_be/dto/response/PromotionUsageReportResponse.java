package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUsageReportResponse {
    private UUID promotionId;
    private String code;
    private String name;
    private Integer totalUsageCount;
    private Integer uniqueCustomerCount;
    private BigDecimal totalDiscountAmount;
    private LocalDateTime lastUsedAt;
    private List<UsageDetailResponse> recentUsages;
}
