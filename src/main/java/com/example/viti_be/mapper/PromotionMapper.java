package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.AppliedPromotionResponse;
import com.example.viti_be.dto.response.PromotionResponse;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.PromotionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct Mapper cho Promotion entities
 *
 * Note: Sử dụng @Mapper(componentModel = "spring") để Spring tự động inject
 * Uses: injection strategy để inject ObjectMapper
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class PromotionMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    // ========== ENTITY TO RESPONSE ==========

    /**
     * Map Promotion entity sang PromotionResponse DTO
     */
    @Mapping(target = "applicableCustomerTiers", source = "promotion", qualifiedByName = "parseCustomerTiers")
    @Mapping(target = "applicableCategoryIds", source = "promotion", qualifiedByName = "extractCategoryIds")
    @Mapping(target = "applicableProductIds", source = "promotion", qualifiedByName = "extractProductIds")
    @Mapping(target = "conflictingPromotionIds", source = "promotion", qualifiedByName = "extractConflictIds")
    @Mapping(target = "isActive", source = "promotion", qualifiedByName = "checkIsActive")
    @Mapping(target = "hasQuota", source = "promotion", qualifiedByName = "checkHasQuota")
    @Mapping(target = "remainingQuota", source = "promotion", qualifiedByName = "calculateRemainingQuota")
    @Mapping(target = "usageCount", expression = "java(promotion.getUsageCount() != null ? promotion.getUsageCount() : 0)")
    public abstract PromotionResponse toResponse(Promotion promotion);

    /**
     * Map list promotions
     */
    public abstract List<PromotionResponse> toResponseList(List<Promotion> promotions);

    // ========== CUSTOM MAPPING METHODS ==========

    /**
     * Parse JSON string thành List<String> cho customer tiers
     */
    @Named("parseCustomerTiers")
    protected List<String> parseCustomerTiers(Promotion promotion) {
        if (promotion.getApplicableCustomerTiers() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    promotion.getApplicableCustomerTiers(),
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Extract category IDs từ PromotionCategory relationships
     */
    @Named("extractCategoryIds")
    protected List<java.util.UUID> extractCategoryIds(Promotion promotion) {
        if (promotion.getPromotionCategories() == null) {
            return Collections.emptyList();
        }

        return promotion.getPromotionCategories().stream()
                .map(pc -> pc.getCategory().getId())
                .collect(Collectors.toList());
    }

    /**
     * Extract product IDs từ PromotionProduct relationships
     */
    @Named("extractProductIds")
    protected List<java.util.UUID> extractProductIds(Promotion promotion) {
        if (promotion.getPromotionProducts() == null) {
            return Collections.emptyList();
        }

        return promotion.getPromotionProducts().stream()
                .map(pp -> pp.getProduct().getId())
                .collect(Collectors.toList());
    }

    /**
     * Extract conflicting promotion IDs
     */
    @Named("extractConflictIds")
    protected List<java.util.UUID> extractConflictIds(Promotion promotion) {
        if (promotion.getConflicts() == null) {
            return Collections.emptyList();
        }

        return promotion.getConflicts().stream()
                .map(pc -> pc.getConflictingPromotion().getId())
                .collect(Collectors.toList());
    }

    /**
     * Check if promotion is currently active
     */
    @Named("checkIsActive")
    protected Boolean checkIsActive(Promotion promotion) {
        return promotion.isActive();
    }

    /**
     * Check if promotion has quota available
     */
    @Named("checkHasQuota")
    protected Boolean checkHasQuota(Promotion promotion) {
        return promotion.hasQuota();
    }

    /**
     * Calculate remaining quota
     */
    @Named("calculateRemainingQuota")
    protected Integer calculateRemainingQuota(Promotion promotion) {
        if (promotion.getUsageLimit() == null) {
            return null; // Unlimited
        }

        int usageCount = promotion.getUsageCount() != null ? promotion.getUsageCount() : 0;
        return Math.max(0, promotion.getUsageLimit() - usageCount);
    }

    // ========== APPLIED PROMOTION RESPONSE ==========

    /**
     * Map Promotion and discount amount sang AppliedPromotionResponse
     */
    public AppliedPromotionResponse toAppliedResponse(Promotion promotion, BigDecimal discountAmount) {
        if (promotion == null) {
            return null;
        }

        String message = buildPromotionMessage(promotion);

        return AppliedPromotionResponse.builder()
                .promotionId(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .type(promotion.getType())
                .discountAmount(discountAmount)
                .message(message)
                .build();
    }

    /**
     * Build human-readable message for promotion
     */
    private String buildPromotionMessage(Promotion promotion) {
        if (promotion.getType() == PromotionType.PERCENTAGE) {
            String message = String.format("Giảm %.0f%%", promotion.getValue());
            if (promotion.getMaxDiscountAmount() != null) {
                message += String.format(" (tối đa %,.0f VND)", promotion.getMaxDiscountAmount());
            }
            return message;
        } else {
            return String.format("Giảm %,.0f VND", promotion.getValue());
        }
    }
}