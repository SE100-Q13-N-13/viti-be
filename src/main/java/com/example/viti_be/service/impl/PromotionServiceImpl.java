package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.ApplyPromotionCodeRequest;
import com.example.viti_be.dto.request.CartItemRequest;
import com.example.viti_be.dto.request.PromotionRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.exception.*;
import com.example.viti_be.mapper.PromotionMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.PromotionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionConflictRepository promotionConflictRepository;
    private final PromotionUsageHistoryRepository usageHistoryRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final OrderItemPromotionRepository orderItemPromotionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final AuditLogService auditLogService;
    private final PromotionMapper mapper;
    private final ObjectMapper objectMapper;

    // ========================================
    // CRUD OPERATIONS (Đã có trong code gốc)
    // ========================================

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request, UUID adminId) {
        validatePromotionRequest(request, null);

        Promotion promotion = new Promotion();
        mapRequestToEntity(request, promotion);
        promotion.setStatus(determineInitialStatus(request.getStartDate(), request.getEndDate()));
        promotion.setUsageCount(0);

        Promotion saved = promotionRepository.save(promotion);
        updatePromotionRelationships(saved, request);

        auditLogService.logSuccess(adminId, AuditModule.PROMOTION, AuditAction.CREATE,
                saved.getId().toString(), "promotion", null,
                String.format("Created promotion: %s", saved.getCode()));

        log.info("Created promotion {} by admin {}", saved.getCode(), adminId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(UUID id, PromotionRequest request, UUID adminId) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("id", id));

        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            throw new InvalidPromotionException(
                    "Cannot update ACTIVE promotion. Please deactivate it first.");
        }

        validatePromotionRequest(request, id);

        String oldValue = String.format("code=%s, value=%s", promotion.getCode(), promotion.getValue());
        mapRequestToEntity(request, promotion);
        promotion.setStatus(determineInitialStatus(request.getStartDate(), request.getEndDate()));

        Promotion updated = promotionRepository.save(promotion);
        updatePromotionRelationships(updated, request);

        auditLogService.logSuccess(adminId, AuditModule.PROMOTION, AuditAction.UPDATE,
                updated.getId().toString(), "promotion", oldValue,
                String.format("code=%s, value=%s", updated.getCode(), updated.getValue()));

        log.info("Updated promotion {} by admin {}", updated.getCode(), adminId);
        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePromotion(UUID id, UUID adminId) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("id", id));

        promotion.setIsDeleted(true);
        promotion.setStatus(PromotionStatus.INACTIVE);
        promotionRepository.save(promotion);

        auditLogService.logSuccess(adminId, AuditModule.PROMOTION, AuditAction.DELETE,
                id.toString(), "promotion", promotion.getCode(), "Deleted promotion");

        log.info("Deleted promotion {} by admin {}", promotion.getCode(), adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("id", id));
        return mapper.toResponse(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAllPromotions(Pageable pageable) {
        Page<Promotion> promotionPage = promotionRepository.findByIsDeletedFalse(pageable);
        return PageResponse.from(promotionPage, mapper::toResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getActivePromotions(Pageable pageable) {
        Page<Promotion> promotionPage = promotionRepository.findActivePromotions(LocalDateTime.now(), pageable);
        return PageResponse.from(promotionPage, mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getApplicablePromotionsForCart(ApplyPromotionCodeRequest request) {
        // 1. Chuẩn bị dữ liệu
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> publicPromotions = promotionRepository.findActivePublicPromotions(now);
        List<Promotion> applicablePromotions = new ArrayList<>();

        // Lấy thông tin Customer (nếu có)
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        // Tính tổng tiền giỏ hàng (để check Min Order Value)
        BigDecimal cartSubtotal = calculateSubtotal(request.getItems());

        // Cache Product -> Category để tránh query DB trong vòng lặp
        // Map<ProductId, CategoryId>
        Map<UUID, UUID> productCategoryMap = preloadProductCategories(request.getItems());

        // 2. Duyệt qua từng promotion để lọc
        for (Promotion p : publicPromotions) {
            // Check 1: Min Order Value
            if (p.getMinOrderValue() != null && cartSubtotal.compareTo(p.getMinOrderValue()) < 0) {
                continue;
            }

            // Check 2: Customer Tier
            if (!isCustomerTierEligible(p, customer)) {
                continue;
            }

            // Check 3: Global Usage Limit
            if (!p.hasQuota()) {
                continue;
            }

            // Check 4: Usage Per Customer Limit
            if (customer != null && p.getUsagePerCustomer() != null) {
                long usage = usageHistoryRepository.countByPromotionIdAndCustomerId(p.getId(), customer.getId());
                if (usage >= p.getUsagePerCustomer()) {
                    continue;
                }
            }

            // Check 5: Scope (Product vs Order)
            boolean isScopeValid = false;
            if (p.getScope() == PromotionScope.ORDER) {
                isScopeValid = true; // Đã pass MinOrderValue ở trên là đủ
            } else if (p.getScope() == PromotionScope.PRODUCT) {
                // Kiểm tra xem trong giỏ có sản phẩm nào thuộc Promotion này không
                // Chỉ cần có ít nhất 1 sản phẩm khớp là Promotion này được coi là "Applicable"
                isScopeValid = request.getItems().stream().anyMatch(item ->
                        isPromotionApplicableToItem(p, item.getProductId(), productCategoryMap)
                );
            }

            if (isScopeValid) {
                applicablePromotions.add(p);
            }
        }

        return applicablePromotions.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Tính tổng tiền giỏ hàng từ request items
     */
    private BigDecimal calculateSubtotal(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Helper: Kiểm tra hạng thành viên
     */
    private boolean isCustomerTierEligible(Promotion promotion, Customer customer) {
        // Nếu promotion không yêu cầu tier -> OK
        if (promotion.getApplicableCustomerTiers() == null || promotion.getApplicableCustomerTiers().isEmpty()) {
            return true;
        }

        // Nếu promotion yêu cầu tier mà khách là Guest (customer == null) -> Fail
        if (customer == null || customer.getTier() == null) {
            return false;
        }

        try {
            // Parse JSON list tiers từ promotion entity
            List<String> allowedTiers = objectMapper.readValue(
                    promotion.getApplicableCustomerTiers(),
                    new TypeReference<List<String>>() {}
            );

            // Check xem tier của khách có nằm trong list cho phép không
            return allowedTiers.contains(customer.getTier().getName());

        } catch (Exception e) {
            log.error("Error parsing customer tiers for promotion {}", promotion.getCode(), e);
            return false; // Fail safe
        }
    }

    /**
     * Helper: Preload Category ID cho các products trong giỏ để tối ưu hiệu năng
     */
    private Map<UUID, UUID> preloadProductCategories(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) return Collections.emptyMap();

        Set<UUID> productIds = items.stream()
                .map(CartItemRequest::getProductId)
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAllById(productIds);

        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p.getCategory() != null ? p.getCategory().getId() : null, // Handle null category
                        (existing, replacement) -> existing // Merge function (không cần thiết vì ID unique)
                ));
    }

    /**
     * Helper: Kiểm tra Promotion có áp dụng cho 1 Item cụ thể không
     */
    private boolean isPromotionApplicableToItem(Promotion p, UUID productId, Map<UUID, UUID> productCategoryMap) {
        // Check 1: Direct Product Match
        boolean productMatch = p.getPromotionProducts().stream()
                .anyMatch(pp -> pp.getProduct().getId().equals(productId));

        if (productMatch) return true;

        // Check 2: Category Match
        UUID categoryId = productCategoryMap.get(productId);
        if (categoryId != null) {
            boolean categoryMatch = p.getPromotionCategories().stream()
                    .anyMatch(pc -> pc.getCategory().getId().equals(categoryId));
            if (categoryMatch) return true;
        }

        return false;
    }

    @Override
    @Transactional
    public PromotionResponse togglePromotionStatus(UUID id, UUID adminId) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("id", id));

        PromotionStatus oldStatus = promotion.getStatus();
        PromotionStatus newStatus;

        if (oldStatus == PromotionStatus.ACTIVE) {
            newStatus = PromotionStatus.INACTIVE;
        } else if (oldStatus == PromotionStatus.INACTIVE || oldStatus == PromotionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate())) {
                newStatus = PromotionStatus.SCHEDULED;
            } else if (now.isAfter(promotion.getEndDate())) {
                throw new InvalidPromotionException("Cannot activate expired promotion");
            } else {
                newStatus = PromotionStatus.ACTIVE;
            }
        } else {
            throw new InvalidPromotionException("Cannot toggle status of EXPIRED promotion");
        }

        promotion.setStatus(newStatus);
        Promotion saved = promotionRepository.save(promotion);

        auditLogService.logSuccess(adminId, AuditModule.PROMOTION, AuditAction.UPDATE,
                id.toString(), "promotion", "status=" + oldStatus, "status=" + newStatus);

        log.info("Toggled promotion {} status: {} -> {} by admin {}",
                promotion.getCode(), oldStatus, newStatus, adminId);
        return mapper.toResponse(saved);
    }

    // ========================================
    // CART OPERATIONS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public CartDiscountCalculationResponse applyPromotionCode(ApplyPromotionCodeRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new PromotionValidationException("Promotion code cannot be empty");
        }

        String codeToApply = request.getCode().trim().toUpperCase();

        Promotion promotion = promotionRepository.findByCodeAndIsDeletedFalse(codeToApply)
                .orElseThrow(() -> new PromotionNotFoundException(codeToApply));

        validatePromotionForApply(promotion, request.getCustomerId());

        List<String> currentCodes = request.getAppliedCodes() != null
                ? new ArrayList<>(request.getAppliedCodes())
                : new ArrayList<>();

        if (currentCodes.stream().anyMatch(c -> c.equalsIgnoreCase(codeToApply))) {
            throw new PromotionValidationException("Promotion code '" + codeToApply + "' is already applied");
        }


        currentCodes.add(codeToApply);

        // Calculate discount với tất cả codes (bao gồm code mới)
        return calculateCartDiscountWithCodes(request, currentCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDiscountCalculationResponse calculateCartDiscount(ApplyPromotionCodeRequest request) {
        List<String> codes = request.getAppliedCodes() != null
                ? new ArrayList<>(request.getAppliedCodes())
                : new ArrayList<>();

        codes = codes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .map(code -> code.trim().toUpperCase())
                .distinct() // Remove duplicates
                .collect(Collectors.toList());

        return calculateCartDiscountWithCodes(request, codes);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDiscountCalculationResponse removePromotionFromCart(String code, ApplyPromotionCodeRequest cartRequest) {
        if (code == null || code.trim().isEmpty()) {
            throw new PromotionValidationException("Code to remove cannot be empty");
        }

        String codeToRemove = code.trim().toUpperCase();

        List<String> currentCodes = cartRequest.getAppliedCodes() != null
                ? new ArrayList<>(cartRequest.getAppliedCodes())
                : new ArrayList<>();

        List<String> remainingCodes = currentCodes.stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(c -> c.trim().toUpperCase())
                .filter(c -> !c.equals(codeToRemove))
                .distinct()
                .collect(Collectors.toList());

        if (remainingCodes.size() == currentCodes.size()) {
            throw new PromotionValidationException("Code '" + code + "' is not applied to cart");
        }

        return calculateCartDiscountWithCodes(cartRequest, remainingCodes);
    }

    /**
     * Core method: Tính toán discount cho cart với danh sách promotion codes
     */
    private CartDiscountCalculationResponse calculateCartDiscountWithCodes(
            ApplyPromotionCodeRequest request, List<String> manualCodes) {

        // [LOG] Bắt đầu tính toán
        log.info("=== START CALCULATE CART DISCOUNT ===");
        log.info("Customer ID: {}", request.getCustomerId());
        log.info("Manual Codes Input: {}", manualCodes); // Quan trọng: Check xem code có thực sự truyền vào đây không

        List<String> warnings = new ArrayList<>();

        // 1. Tính subtotal
        BigDecimal subtotal = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("1. Calculated Subtotal: {}", subtotal);

        // 2. Tier discount (nếu có customer)
        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null && customer.getTier() != null) {
                BigDecimal rate = customer.getTier().getDiscountRate();
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    tierDiscount = subtotal.multiply(rate)
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
                    log.info("2. Tier Discount Applied: {} (Tier: {}, Rate: {}%)",
                            tierDiscount, customer.getTier().getName(), rate);
                } else {
                    log.info("2. Customer found but Tier Rate is 0 or null");
                }
            } else {
                log.info("2. Customer not found or has no Tier assigned");
            }
        } else {
            log.info("2. Guest Customer - No Tier Discount");
        }

        // 3. Apply PRODUCT promotions
        List<AppliedPromotionResponse> productPromotions = new ArrayList<>();
        BigDecimal productPromotionDiscount = BigDecimal.ZERO;

        log.info("3. Processing Product Promotions for {} items...", request.getItems().size());

        for (CartItemRequest item : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getProductVariantId()).orElse(null);
            if (variant == null) {
                warnings.add("Product variant not found: " + item.getProductVariantId());
                log.warn("Item skipped: Variant {} not found", item.getProductVariantId());
                continue;
            }

            Product product = variant.getProduct();
            UUID productId = product.getId();
            UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

            // Tìm applicable product promotions
            List<Promotion> applicablePromotions = findApplicableProductPromotions(
                    productId, categoryId, request.getCustomerId(), manualCodes);

            log.info("   > Item {} (Product {}): Found {} applicable candidates",
                    variant.getId(), productId, applicablePromotions.size());

            // Resolve conflicts và apply
            List<Promotion> selectedPromotions = resolveConflictsAndSelectPromotions(
                    applicablePromotions, manualCodes);

            if (selectedPromotions.isEmpty() && !applicablePromotions.isEmpty()) {
                log.warn("   > All candidates for Item {} were filtered out by Conflict Resolver!", variant.getId());
            }

            // Tính discount cho item này
            BigDecimal itemBaseAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            for (Promotion promo : selectedPromotions) {
                BigDecimal itemDiscount = promo.calculateDiscount(itemBaseAmount);
                productPromotionDiscount = productPromotionDiscount.add(itemDiscount);

                log.info("   > Applied Promo '{}' to Item {}. Discount: {}",
                        promo.getCode(), variant.getId(), itemDiscount);

                // Add to response (chỉ add 1 lần cho mỗi promotion)
                boolean alreadyAdded = productPromotions.stream()
                        .anyMatch(p -> p.getPromotionId().equals(promo.getId()));
                if (!alreadyAdded) {
                    productPromotions.add(mapper.toAppliedResponse(promo, itemDiscount));
                }
            }
        }
        log.info("   > Total Product Discount: {}", productPromotionDiscount);

        // 4. Apply ORDER promotion (chỉ 1 cái tốt nhất)
        AppliedPromotionResponse orderPromotion = null;
        BigDecimal orderPromotionDiscount = BigDecimal.ZERO;

        BigDecimal baseForOrderPromo = subtotal.subtract(tierDiscount).subtract(productPromotionDiscount);
        log.info("4. Processing Order Promotions. Base Amount: {}", baseForOrderPromo);

        if (baseForOrderPromo.compareTo(BigDecimal.ZERO) > 0) {
            List<Promotion> orderPromotions = findApplicableOrderPromotions(
                    baseForOrderPromo, request.getCustomerId(), manualCodes);

            log.info("   > Found {} applicable Order Promos candidates", orderPromotions.size());

            if (!orderPromotions.isEmpty()) {
                // Chọn promotion có discount lớn nhất
                Promotion bestOrderPromo = orderPromotions.stream()
                        .max(Comparator.comparing(p -> p.calculateDiscount(baseForOrderPromo)))
                        .orElse(null);

                if (bestOrderPromo != null) {
                    orderPromotionDiscount = bestOrderPromo.calculateDiscount(baseForOrderPromo);
                    orderPromotion = mapper.toAppliedResponse(bestOrderPromo, orderPromotionDiscount);
                    log.info("   > Selected Best Promo: '{}' (Discount: {})",
                            bestOrderPromo.getCode(), orderPromotionDiscount);
                }
            } else {
                log.info("   > No applicable Order Promos found (Check MinOrderValue, Dates, Usage Limit)");
            }
        } else {
            log.info("   > Base Amount <= 0, skipping Order Promotions");
        }

        // 5. Total discount
        BigDecimal totalDiscount = tierDiscount
                .add(productPromotionDiscount)
                .add(orderPromotionDiscount);

        // 6. Final amount
        BigDecimal finalAmount = subtotal.subtract(totalDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        log.info("=== END CALCULATION ===");
        log.info("Subtotal: {}, Total Discount: {}, Final: {}", subtotal, totalDiscount, finalAmount);

        return CartDiscountCalculationResponse.builder()
                .subtotal(subtotal)
                .tierDiscount(tierDiscount)
                .productPromotionDiscount(productPromotionDiscount)
                .orderPromotionDiscount(orderPromotionDiscount)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .productPromotions(productPromotions)
                .orderPromotion(orderPromotion)
                .warnings(warnings)
                .build();
    }
    // ========================================
    // ORDER INTEGRATION
    // ========================================

    @Override
    @Transactional
    public void applyPromotionsToOrder(Order order, List<String> promotionCodes, UUID actorId) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        // Convert order items to cart items
        ApplyPromotionCodeRequest cartRequest = buildCartRequestFromOrder(order);

        // Calculate discounts
        CartDiscountCalculationResponse calculation = calculateCartDiscountWithCodes(
                cartRequest, promotionCodes != null ? promotionCodes : Collections.emptyList());

        // Apply product promotions to order items
        applyProductPromotionsToOrderItems(order, calculation.getProductPromotions());

        // Apply order promotion
        if (calculation.getOrderPromotion() != null) {
            applyOrderPromotionToOrder(order, calculation.getOrderPromotion());
        }

        // Update order totals
        updateOrderTotals(order, calculation);

        log.info("Applied promotions to order {}: {} product promos, {} order promo",
                order.getOrderNumber(),
                calculation.getProductPromotions().size(),
                calculation.getOrderPromotion() != null ? 1 : 0);
    }

    @Override
    @Transactional
    public void validateAndConfirmPromotions(Order order, UUID actorId) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        // Re-validate all applied promotions
        List<OrderPromotion> orderPromotions = orderPromotionRepository.findByOrderId(order.getId());
        List<OrderItemPromotion> itemPromotions = order.getItems().stream()
                .flatMap(item -> orderItemPromotionRepository.findByOrderItemId(item.getId()).stream())
                .toList();

        // Check if promotions are still valid
        LocalDateTime now = LocalDateTime.now();
        List<UUID> invalidPromotionIds = getUuids(orderPromotions, itemPromotions);

        if (!invalidPromotionIds.isEmpty()) {
            throw new InvalidPromotionException(
                    "Some promotions are no longer valid: " + invalidPromotionIds);
        }

        // Increment usage count và tạo usage history
        Set<UUID> processedPromotions = new HashSet<>();

        for (OrderPromotion op : orderPromotions) {
            Promotion promo = op.getPromotion();
            if (processedPromotions.add(promo.getId())) {
                promo.incrementUsage();
                promotionRepository.save(promo);

                // Create usage history
                createUsageHistory(promo, order, op.getDiscountAmount(), actorId);
            }
        }

        for (OrderItemPromotion ip : itemPromotions) {
            Promotion promo = ip.getPromotion();
            if (processedPromotions.add(promo.getId())) {
                promo.incrementUsage();
                promotionRepository.save(promo);

                // Create usage history
                createUsageHistory(promo, order, ip.getDiscountAmount(), actorId);
            }
        }

        log.info("Validated and confirmed promotions for order {}", order.getOrderNumber());
    }

    private static List<UUID> getUuids(List<OrderPromotion> orderPromotions, List<OrderItemPromotion> itemPromotions) {
        List<UUID> invalidPromotionIds = new ArrayList<>();

        for (OrderPromotion op : orderPromotions) {
            Promotion promo = op.getPromotion();
            if (!promo.isActive() || !promo.hasQuota()) {
                invalidPromotionIds.add(promo.getId());
            }
        }

        for (OrderItemPromotion ip : itemPromotions) {
            Promotion promo = ip.getPromotion();
            if (!promo.isActive() || !promo.hasQuota()) {
                invalidPromotionIds.add(promo.getId());
            }
        }
        return invalidPromotionIds;
    }

    @Override
    @Transactional
    public void restorePromotionUsage(Order order, UUID actorId) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        // Get all promotions used in this order
        Set<UUID> promotionIds = new HashSet<>();

        List<OrderPromotion> orderPromotions = orderPromotionRepository.findByOrderId(order.getId());
        orderPromotions.forEach(op -> promotionIds.add(op.getPromotion().getId()));

        order.getItems().forEach(item -> {
            List<OrderItemPromotion> itemPromotions = orderItemPromotionRepository
                    .findByOrderItemId(item.getId());
            itemPromotions.forEach(ip -> promotionIds.add(ip.getPromotion().getId()));
        });

        // Decrement usage count
        for (UUID promoId : promotionIds) {
            Promotion promo = promotionRepository.findById(promoId).orElse(null);
            if (promo != null) {
                promo.decrementUsage();
                promotionRepository.save(promo);
            }
        }

        // Delete usage history
        usageHistoryRepository.deleteByOrderId(order.getId());

        log.info("Restored promotion usage for cancelled order {}", order.getOrderNumber());
    }

    // ========================================
    // SCHEDULED JOBS
    // ========================================

    @Override
    @Transactional
    public void autoActivatePromotions() {
        List<Promotion> toActivate = promotionRepository.findPromotionsToActivate(LocalDateTime.now());
        for (Promotion p : toActivate) {
            p.setStatus(PromotionStatus.ACTIVE);
            promotionRepository.save(p);
            log.info("Auto-activated promotion: {}", p.getCode());
        }
    }

    @Override
    @Transactional
    public void autoExpirePromotions() {
        List<Promotion> toExpire = promotionRepository.findPromotionsToExpire(LocalDateTime.now());
        for (Promotion p : toExpire) {
            p.setStatus(PromotionStatus.EXPIRED);
            promotionRepository.save(p);
            log.info("Auto-expired promotion: {}", p.getCode());
        }
    }

    // ========================================
    // REPORTS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public PromotionUsageReportResponse getPromotionUsageReport(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException("id", promotionId));

        List<PromotionUsageHistory> histories = usageHistoryRepository
                .findByPromotionIdOrderByUsedAtDesc(promotionId);

        int uniqueCustomers = (int) histories.stream()
                .filter(h -> h.getCustomer() != null)
                .map(h -> h.getCustomer().getId())
                .distinct()
                .count();

        BigDecimal totalDiscount = histories.stream()
                .map(PromotionUsageHistory::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime lastUsed = histories.isEmpty() ? null : histories.get(0).getUsedAt();

        List<UsageDetailResponse> recentUsages = histories.stream()
                .limit(10)
                .map(h -> UsageDetailResponse.builder()
                        .orderId(h.getOrder().getId())
                        .orderNumber(h.getOrder().getOrderNumber())
                        .customerId(h.getCustomer() != null ? h.getCustomer().getId() : null)
                        .customerName(h.getCustomer() != null ? h.getCustomer().getFullName() : "Guest")
                        .discountAmount(h.getDiscountAmount())
                        .usedAt(h.getUsedAt())
                        .build())
                .collect(Collectors.toList());

        return PromotionUsageReportResponse.builder()
                .promotionId(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .totalUsageCount(promotion.getUsageCount() != null ? promotion.getUsageCount() : 0)
                .uniqueCustomerCount(uniqueCustomers)
                .totalDiscountAmount(totalDiscount)
                .lastUsedAt(lastUsed)
                .recentUsages(recentUsages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionUsageReportResponse> getTopPromotions(int limit) {
        List<Promotion> allPromotions = promotionRepository.findByIsDeletedFalse();

        return allPromotions.stream()
                .sorted(Comparator.comparing(p ->
                                p.getUsageCount() != null ? p.getUsageCount() : 0,
                        Comparator.reverseOrder()))
                .limit(limit)
                .map(p -> {
                    List<PromotionUsageHistory> histories = usageHistoryRepository
                            .findByPromotionIdOrderByUsedAtDesc(p.getId());

                    BigDecimal totalDiscount = histories.stream()
                            .map(PromotionUsageHistory::getDiscountAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int uniqueCustomers = (int) histories.stream()
                            .filter(h -> h.getCustomer() != null)
                            .map(h -> h.getCustomer().getId())
                            .distinct()
                            .count();

                    return PromotionUsageReportResponse.builder()
                            .promotionId(p.getId())
                            .code(p.getCode())
                            .name(p.getName())
                            .totalUsageCount(p.getUsageCount() != null ? p.getUsageCount() : 0)
                            .uniqueCustomerCount(uniqueCustomers)
                            .totalDiscountAmount(totalDiscount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private void validatePromotionRequest(PromotionRequest request, UUID excludeId) {
        promotionRepository.findByCodeAndIsDeletedFalse(request.getCode().toUpperCase())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(excludeId)) {
                        throw new DuplicatePromotionCodeException(request.getCode());
                    }
                });

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new PromotionValidationException("End date must be after start date");
        }

        if (request.getEndDate().isBefore(LocalDateTime.now())) {
            throw new PromotionValidationException("Cannot create promotion with end date in the past");
        }

        if (request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PromotionValidationException("Promotion value must be positive");
        }

        if (request.getType() == PromotionType.PERCENTAGE &&
                request.getValue().compareTo(new BigDecimal(100)) > 0) {
            throw new PromotionValidationException("Percentage value cannot exceed 100%");
        }

        if (request.getScope() == PromotionScope.PRODUCT) {
            if ((request.getApplicableCategoryIds() == null || request.getApplicableCategoryIds().isEmpty()) &&
                    (request.getApplicableProductIds() == null || request.getApplicableProductIds().isEmpty())) {
                throw new PromotionValidationException(
                        "PRODUCT promotion must have at least one category or product");
            }
        }
    }

    private void validatePromotionForApply(Promotion promotion, UUID customerId) {
        if (!promotion.isActive()) {
            throw new InvalidPromotionException("Promotion is not active: " + promotion.getCode());
        }

        if (!promotion.hasQuota()) {
            throw new PromotionQuotaExceededException(promotion.getCode());
        }

        // Check customer tier restriction
        if (customerId != null && promotion.getApplicableCustomerTiers() != null) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer != null && customer.getTier() != null) {
                try {
                    List<String> allowedTiers = objectMapper.readValue(
                            promotion.getApplicableCustomerTiers(),
                            new TypeReference<List<String>>() {});

                    String customerTierName = customer.getTier().getName();
                    if (!allowedTiers.contains(customerTierName)) {
                        throw new PromotionNotApplicableException(
                                "Promotion not applicable for your tier: " + customerTierName);
                    }
                } catch (Exception e) {
                    log.error("Error parsing customer tiers", e);
                }
            }
        }

        // Check usage per customer limit
        if (customerId != null && promotion.getUsagePerCustomer() != null) {
            long customerUsage = usageHistoryRepository.countByPromotionIdAndCustomerId(
                    promotion.getId(), customerId);

            if (customerUsage >= promotion.getUsagePerCustomer()) {
                throw new PromotionQuotaExceededException(
                        "You have reached the usage limit for this promotion");
            }
        }
    }

    private PromotionStatus determineInitialStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) {
            return PromotionStatus.SCHEDULED;
        } else if (now.isAfter(endDate)) {
            return PromotionStatus.EXPIRED;
        } else {
            return PromotionStatus.ACTIVE;
        }
    }

    private void mapRequestToEntity(PromotionRequest request, Promotion promotion) {
        promotion.setCode(request.getCode().toUpperCase());
        promotion.setName(request.getName());
        promotion.setType(request.getType());
        promotion.setScope(request.getScope());
        promotion.setValue(request.getValue());
        promotion.setMinOrderValue(request.getMinOrderValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsagePerCustomer(request.getUsagePerCustomer());
        promotion.setPriority(request.getPriority());
        promotion.setDescription(request.getDescription());

        if (request.getApplicableCustomerTiers() != null && !request.getApplicableCustomerTiers().isEmpty()) {
            try {
                promotion.setApplicableCustomerTiers(
                        objectMapper.writeValueAsString(request.getApplicableCustomerTiers()));
            } catch (Exception e) {
                throw new PromotionValidationException("Failed to serialize customer tiers");
            }
        } else {
            promotion.setApplicableCustomerTiers(null);
        }
    }

    private void updatePromotionRelationships(Promotion promotion, PromotionRequest request) {
        promotionCategoryRepository.deleteByPromotionId(promotion.getId());
        promotionProductRepository.deleteByPromotionId(promotion.getId());
        promotionConflictRepository.deleteByPromotionId(promotion.getId());

        if (request.getApplicableCategoryIds() != null) {
            for (UUID categoryId : request.getApplicableCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                PromotionCategory pc = PromotionCategory.builder()
                        .promotion(promotion)
                        .category(category)
                        .build();
                promotionCategoryRepository.save(pc);
            }
        }

        if (request.getApplicableProductIds() != null) {
            for (UUID productId : request.getApplicableProductIds()) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                PromotionProduct pp = PromotionProduct.builder()
                        .promotion(promotion)
                        .product(product)
                        .build();
                promotionProductRepository.save(pp);
            }
        }

        if (request.getConflictingPromotionIds() != null) {
            for (UUID conflictId : request.getConflictingPromotionIds()) {
                Promotion conflicting = promotionRepository.findById(conflictId)
                        .orElseThrow(() -> new RuntimeException("Conflicting promotion not found: " + conflictId));
                PromotionConflict pc = PromotionConflict.builder()
                        .promotion(promotion)
                        .conflictingPromotion(conflicting)
                        .build();
                promotionConflictRepository.save(pc);
            }
        }
    }

    private List<Promotion> findApplicableProductPromotions(
            UUID productId, UUID categoryId, UUID customerId, List<String> manualCodes) {

        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findApplicableProductPromotions(
                productId, categoryId, now);

        Set<String> manualCodesUpper = manualCodes.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return promotions.stream()
                .filter(p -> {
                    if (Boolean.TRUE.equals(p.getRequiresCode())) {
                        // Chỉ apply nếu code được nhập
                        if (!manualCodesUpper.contains(p.getCode())) {
                            return false;
                        }
                    }
                    // Check min order value (skip for product promotions)
                    // Check tier restriction
                    if (customerId == null && p.getApplicableCustomerTiers() != null) {
                        return false; // Guest cannot use tier-restricted promotions
                    }
                    // Check if quota available
                    if (!p.hasQuota()) {
                        return false;
                    }
                    // Check usage per customer
                    if (customerId != null && p.getUsagePerCustomer() != null) {
                        long usage = usageHistoryRepository.countByPromotionIdAndCustomerId(
                                p.getId(), customerId);
                        return usage < p.getUsagePerCustomer();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Promotion> findApplicableOrderPromotions(
            BigDecimal orderAmount, UUID customerId, List<String> manualCodes) {

        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findApplicableOrderPromotions(now);

        Set<String> manualCodesUpper = manualCodes.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return promotions.stream()
                .filter(p -> {
                    if (Boolean.TRUE.equals(p.getRequiresCode())) {
                        // Chỉ apply nếu code được nhập
                        if (!manualCodesUpper.contains(p.getCode())) {
                            return false;
                        }
                    }
                    // Check min order value
                    if (p.getMinOrderValue() != null &&
                            orderAmount.compareTo(p.getMinOrderValue()) < 0) {
                        return false;
                    }
                    // Check tier restriction
                    if (customerId == null && p.getApplicableCustomerTiers() != null) {
                        return false;
                    }
                    // Check quota
                    if (!p.hasQuota()) {
                        return false;
                    }
                    // Check usage per customer
                    if (customerId != null && p.getUsagePerCustomer() != null) {
                        long usage = usageHistoryRepository.countByPromotionIdAndCustomerId(
                                p.getId(), customerId);
                        return usage < p.getUsagePerCustomer();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Promotion> resolveConflictsAndSelectPromotions(
            List<Promotion> promotions, List<String> manualCodes) {

        if (promotions.isEmpty()) {
            return Collections.emptyList();
        }

        // Build conflict map
        Map<UUID, Set<UUID>> conflictMap = new HashMap<>();
        for (Promotion p : promotions) {
            List<UUID> conflicts = promotionConflictRepository.findConflictingPromotionIds(p.getId());
            conflictMap.put(p.getId(), new HashSet<>(conflicts));
        }

        // Select promotions without conflicts
        List<Promotion> selected = new ArrayList<>();
        Set<UUID> selectedIds = new HashSet<>();

        // Sort by priority (higher first)
        List<Promotion> sorted = promotions.stream()
                .sorted(Comparator.comparing(Promotion::getPriority).reversed())
                .collect(Collectors.toList());

        for (Promotion p : sorted) {
            // Check if this promotion conflicts with any selected promotion
            boolean hasConflict = selectedIds.stream()
                    .anyMatch(selectedId -> {
                        Set<UUID> conflicts = conflictMap.get(p.getId());
                        return conflicts != null && conflicts.contains(selectedId);
                    });

            if (!hasConflict) {
                selected.add(p);
                selectedIds.add(p.getId());
            }
        }

        return selected;
    }

    private ApplyPromotionCodeRequest buildCartRequestFromOrder(Order order) {
        List<CartItemRequest> items = order.getItems().stream()
                .map(item -> CartItemRequest.builder()
                        .productId(item.getProduct().getId())
                        .productVariantId(item.getProductVariant().getId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return ApplyPromotionCodeRequest.builder()
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .items(items)
                .build();
    }

    private void applyProductPromotionsToOrderItems(Order order, List<AppliedPromotionResponse> promotions) {
        for (AppliedPromotionResponse promoResponse : promotions) {
            Promotion promotion = promotionRepository.findById(promoResponse.getPromotionId())
                    .orElse(null);
            if (promotion == null) continue;

            for (OrderItem item : order.getItems()) {
                // Check if promotion applies to this item
                UUID productId = item.getProduct().getId();
                UUID categoryId = item.getProduct().getCategory() != null ?
                        item.getProduct().getCategory().getId() : null;

                boolean applies = promotion.getPromotionProducts().stream()
                        .anyMatch(pp -> pp.getProduct().getId().equals(productId))
                        || (categoryId != null && promotion.getPromotionCategories().stream()
                        .anyMatch(pc -> pc.getCategory().getId().equals(categoryId)));

                if (applies) {
                    BigDecimal baseAmount = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal discount = promotion.calculateDiscount(baseAmount);

                    OrderItemPromotion itemPromo = OrderItemPromotion.builder()
                            .orderItem(item)
                            .promotion(promotion)
                            .discountAmount(discount)
                            .baseAmount(baseAmount)
                            .build();

                    orderItemPromotionRepository.save(itemPromo);
                }
            }
        }
    }

    private void applyOrderPromotionToOrder(Order order, AppliedPromotionResponse promoResponse) {
        Promotion promotion = promotionRepository.findById(promoResponse.getPromotionId())
                .orElse(null);
        if (promotion == null) return;

        OrderPromotion orderPromo = OrderPromotion.builder()
                .order(order)
                .promotion(promotion)
                .discountAmount(promoResponse.getDiscountAmount())
                .baseAmount(order.getSubtotal())
                .build();

        orderPromotionRepository.save(orderPromo);
    }

    private void updateOrderTotals(Order order, CartDiscountCalculationResponse calculation) {
        // Note: Order totals are calculated in OrderService
        // This method can be used if needed
    }

    private void createUsageHistory(Promotion promotion, Order order, BigDecimal discountAmount, UUID actorId) {
        PromotionUsageHistory history = PromotionUsageHistory.builder()
                .promotion(promotion)
                .customer(order.getCustomer())
                .order(order)
                .discountAmount(discountAmount)
                .usedAt(LocalDateTime.now())
                .build();

        usageHistoryRepository.save(history);
    }
}