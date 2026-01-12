package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.PromotionScope;
import com.example.viti_be.model.model_enum.PromotionType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric with underscores")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Type is required")
    private PromotionType type;

    @NotNull(message = "Scope is required")
    private PromotionScope scope;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.00", message = "Value must be non-negative")
    private BigDecimal value;

    @DecimalMin(value = "0.00", message = "Min order value must be non-negative")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0.00", message = "Max discount amount must be non-negative")
    private BigDecimal maxDiscountAmount;

    private List<String> applicableCustomerTiers; // ["GOLD", "PLATINUM"]

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Usage per customer must be at least 1")
    private Integer usagePerCustomer;

    @NotNull(message = "Priority is required")
    @Min(value = 0, message = "Priority must be non-negative")
    private Integer priority;

    private String description;

    private List<UUID> applicableCategoryIds;
    private List<UUID> applicableProductIds;
    private List<UUID> conflictingPromotionIds;
}