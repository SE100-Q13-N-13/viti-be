package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private UUID id;
    private String code;
    private String name;
    private PromotionType type;
    private PromotionScope scope;
    private BigDecimal value;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private List<String> applicableCustomerTiers;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PromotionStatus status;
    private Integer usageLimit;
    private Integer usageCount;
    private Integer usagePerCustomer;
    private Integer priority;
    private String description;

    // Relationships
    private List<UUID> applicableCategoryIds;
    private List<UUID> applicableProductIds;
    private List<UUID> conflictingPromotionIds;

    // Computed fields
    private Boolean isActive;
    private Boolean hasQuota;
    private Integer remainingQuota;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}