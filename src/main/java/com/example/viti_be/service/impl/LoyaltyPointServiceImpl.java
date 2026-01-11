package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.AdjustPointsRequest;
import com.example.viti_be.dto.request.ResetPointsRequest;
import com.example.viti_be.dto.request.UpdateLoyaltyConfigRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.mapper.LoyaltyPointMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.model.model_enum.TransactionType;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.LoyaltyPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPointServiceImpl implements LoyaltyPointService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyPointTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogService auditLogService;
    private final LoyaltyPointMapper mapper;

    // ========== CONFIG KEYS ==========
    private static final String EARN_ENABLED = "loyalty.earn.enabled";
    private static final String EARN_RATE = "loyalty.earn.rate";
    private static final String MIN_ORDER_TO_EARN = "loyalty.earn.min_order_amount";
    private static final String REDEEM_ENABLED = "loyalty.redeem.enabled";
    private static final String REDEEM_RATE = "loyalty.redeem.rate";
    private static final String MAX_REDEEM_PERCENT = "loyalty.redeem.max_percent";
    private static final String MIN_ORDER_TO_REDEEM = "loyalty.redeem.min_order_amount";

    // ========== CUSTOMER OPERATIONS ==========

    @Override
    @Transactional(readOnly = true)
    public LoyaltyPointResponse getCustomerPoints(UUID customerId) {
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        CustomerTier currentTier = getTierByPoints(loyaltyPoint.getTotalPoints());
        Integer pointsToNext = getPointsToNextTier(loyaltyPoint.getTotalPoints());

        return mapper.toResponse(loyaltyPoint, currentTier.getName(), pointsToNext);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoyaltyPointTransactionResponse> getTransactionHistory(UUID customerId, Pageable pageable) {
        return transactionRepository.findByCustomerId(customerId, pageable)
                .map(mapper::toTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getDetailedTransactionHistory(UUID customerId, Pageable pageable) {
        LoyaltyPointResponse currentStatus = getCustomerPoints(customerId);
        Page<LoyaltyPointTransactionResponse> transactions = getTransactionHistory(customerId, pageable);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Integer earnedThisMonth = transactionRepository.sumEarnedPointsByCustomerAndDateRange(
                customerId, startOfMonth, endOfMonth);
        Integer redeemedThisMonth = transactionRepository.sumRedeemedPointsByCustomerAndDateRange(
                customerId, startOfMonth, endOfMonth);

        return TransactionHistoryResponse.builder()
                .currentStatus(currentStatus)
                .transactions(transactions.getContent())
                .totalEarnedThisMonth(earnedThisMonth != null ? earnedThisMonth : 0)
                .totalRedeemedThisMonth(redeemedThisMonth != null ? redeemedThisMonth : 0)
                .build();
    }

    // ========== EARN & REDEEM OPERATIONS ==========

    @Override
    @Transactional
    public void earnPointsFromOrder(Order order, UUID employeeId) {
        // 1. Validate
        if (!getConfigBoolean(EARN_ENABLED, true)) {
            log.info("Loyalty earning is disabled");
            return;
        }

        if (transactionRepository.existsByOrderIdAndTransactionTypeAndIsDeletedFalse(
                order.getId(), TransactionType.EARN)) {
            log.warn("Order {} already earned points", order.getId());
            return;
        }

        BigDecimal minOrderAmount = getConfigBigDecimal(MIN_ORDER_TO_EARN, BigDecimal.ZERO);
        if (order.getSubtotal() == null || order.getSubtotal().compareTo(minOrderAmount) < 0) {
            log.info("Order amount {} below minimum {}", order.getSubtotal(), minOrderAmount);
            return;
        }

        // 2. Calculate earn points
        // Base = Subtotal - Total Discount + Points Used (để lấy giá trị trước khi dùng điểm)
        BigDecimal baseAmount = order.getSubtotal()
                .subtract(order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO);

        // Nếu đơn có dùng điểm, cộng lại số tiền đã trừ để tính điểm trên giá gốc
        if (order.getLoyaltyPointsUsed() > 0) {
            BigDecimal pointsValue = calculateRedemptionAmount(order.getLoyaltyPointsUsed());
            baseAmount = baseAmount.add(pointsValue);
        }

        Integer earnRate = getConfigInt(EARN_RATE, 100000); // Default: 100,000 VND = 1 point
        Integer pointsEarned = baseAmount.divide(new BigDecimal(earnRate), 0, RoundingMode.DOWN).intValue();

        if (pointsEarned <= 0) {
            log.info("No points to earn for order {}", order.getId());
            return;
        }

        // 3. Get or create loyalty point
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(order.getCustomer().getId())
                .orElseGet(() -> createLoyaltyPointForCustomer(order.getCustomer()));

        // 4. Update loyalty point
        loyaltyPoint.setTotalPoints(loyaltyPoint.getTotalPoints() + pointsEarned);
        loyaltyPoint.setPointsAvailable(loyaltyPoint.getPointsAvailable() + pointsEarned);
        loyaltyPoint.setLastEarnedAt(LocalDateTime.now());
        loyaltyPointRepository.save(loyaltyPoint);

        // 5. Create transaction record
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createEarnTransaction(
                loyaltyPoint, order, pointsEarned, employee);
        transactionRepository.save(transaction);

        // 6. Update customer tier
        updateCustomerTier(order.getCustomer().getId());

        // 7. Audit log
        auditLogService.logSuccess(
                employeeId,
                AuditModule.LOYALTY_POINTS,
                AuditAction.EARN_POINTS,
                loyaltyPoint.getId().toString(),
                "loyaltyPoint",
                null,
                String.format("Earned %d points from order %s", pointsEarned, order.getId())
        );

        log.info("Customer {} earned {} points from order {}",
                order.getCustomer().getId(), pointsEarned, order.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRedemption(UUID customerId, Integer pointsToRedeem, BigDecimal orderSubtotal) {
        if (!getConfigBoolean(REDEEM_ENABLED, true)) {
            throw new RuntimeException("Loyalty redemption is currently disabled");
        }

        if (pointsToRedeem == null || pointsToRedeem <= 0) {
            throw new IllegalArgumentException("Points to redeem must be positive");
        }

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer has no loyalty points"));

        if (pointsToRedeem > loyaltyPoint.getPointsAvailable()) {
            throw new RuntimeException(String.format(
                    "Insufficient points. Available: %d, Requested: %d",
                    loyaltyPoint.getPointsAvailable(), pointsToRedeem));
        }

        BigDecimal minOrderAmount = getConfigBigDecimal(MIN_ORDER_TO_REDEEM, BigDecimal.ZERO);
        if (orderSubtotal.compareTo(minOrderAmount) < 0) {
            throw new RuntimeException(String.format(
                    "Order amount %s below minimum %s to redeem points",
                    orderSubtotal, minOrderAmount));
        }

        Integer maxPercent = getConfigInt(MAX_REDEEM_PERCENT, 0);
        if (maxPercent > 0) {
            BigDecimal maxRedeemAmount = orderSubtotal
                    .multiply(new BigDecimal(maxPercent))
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
            BigDecimal requestedAmount = calculateRedemptionAmount(pointsToRedeem);

            if (requestedAmount.compareTo(maxRedeemAmount) > 0) {
                throw new RuntimeException(String.format(
                        "Can only redeem up to %d%% of order value (%s VND)",
                        maxPercent, maxRedeemAmount));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRedemptionAmount(Integer points) {
        Integer redeemRate = getConfigInt(REDEEM_RATE, 1000); // Default: 1 point = 1,000 VND
        return new BigDecimal(points).multiply(new BigDecimal(redeemRate));
    }

    // ========== ADMIN OPERATIONS ==========

    @Override
    @Transactional
    public LoyaltyPointResponse adjustPoints(UUID customerId, AdjustPointsRequest request, UUID adminId) {
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        Integer oldTotal = loyaltyPoint.getTotalPoints();
        Integer oldAvailable = loyaltyPoint.getPointsAvailable();

        // Validate không để points_available âm
        if (request.getPointsChange() < 0 &&
                Math.abs(request.getPointsChange()) > loyaltyPoint.getPointsAvailable()) {
            throw new RuntimeException("Cannot reduce more points than available");
        }

        // Update points
        loyaltyPoint.setTotalPoints(loyaltyPoint.getTotalPoints() + request.getPointsChange());
        loyaltyPoint.setPointsAvailable(loyaltyPoint.getPointsAvailable() + request.getPointsChange());

        if (request.getPointsChange() > 0) {
            loyaltyPoint.setLastEarnedAt(LocalDateTime.now());
        } else {
            loyaltyPoint.setLastUsedAt(LocalDateTime.now());
        }

        loyaltyPointRepository.save(loyaltyPoint);

        // Create transaction
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createManualAdjustTransaction(
                loyaltyPoint, request.getPointsChange(), request.getReason(), admin);
        transactionRepository.save(transaction);

        // Update tier
        updateCustomerTier(customerId);

        // Audit
        auditLogService.logSuccess(
                adminId,
                AuditModule.LOYALTY_POINTS,
                AuditAction.ADJUST_POINTS,
                loyaltyPoint.getId().toString(),
                "loyaltyPoint",
                String.format("total=%d, available=%d", oldTotal, oldAvailable),
                String.format("total=%d, available=%d, change=%+d, reason=%s",
                        loyaltyPoint.getTotalPoints(), loyaltyPoint.getPointsAvailable(),
                        request.getPointsChange(), request.getReason())
        );

        CustomerTier currentTier = getTierByPoints(loyaltyPoint.getTotalPoints());
        Integer pointsToNext = getPointsToNextTier(loyaltyPoint.getTotalPoints());
        return mapper.toResponse(loyaltyPoint, currentTier.getName(), pointsToNext);
    }

    // ========== RESET OPERATIONS ==========

    @Override
    @Transactional
    public LoyaltyPointResponse resetCustomerPoints(UUID customerId, ResetPointsRequest request, UUID adminId) {
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        Integer oldTotal = loyaltyPoint.getTotalPoints();
        Integer oldAvailable = loyaltyPoint.getPointsAvailable();

        // Reset points
        loyaltyPoint.setTotalPoints(0);
        loyaltyPoint.setPointsAvailable(0);
        loyaltyPoint.setPointsUsed(0);
        loyaltyPointRepository.save(loyaltyPoint);

        // Create transaction
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createResetTransaction(
                loyaltyPoint, request.getReason(), admin);
        transactionRepository.save(transaction);

        // Update tier to lowest (REGULAR)
        Customer customer = loyaltyPoint.getCustomer();
        CustomerTier regularTier = getTierByPoints(0);
        customer.setTier(regularTier);
        customerRepository.save(customer);

        // Audit
        auditLogService.logSuccess(
                adminId,
                AuditModule.LOYALTY_POINTS,
                AuditAction.RESET_POINTS,
                loyaltyPoint.getId().toString(),
                "loyaltyPoint",
                String.format("total=%d, available=%d", oldTotal, oldAvailable),
                String.format("Reset to 0. Reason: %s", request.getReason())
        );

        return mapper.toResponse(loyaltyPoint, regularTier.getName(), getPointsToNextTier(0));
    }

    @Override
    @Transactional
    public ResetResultResponse resetAllCustomerPoints(String reason, UUID adminId) {
        List<LoyaltyPoint> allPoints = loyaltyPointRepository.findAll();
        Integer totalCustomers = 0;
        Integer totalPointsReset = 0;

        User admin = adminId != null ? userRepository.findById(adminId).orElse(null) : null;
        CustomerTier regularTier = getTierByPoints(0);

        for (LoyaltyPoint lp : allPoints) {
            if (lp.getTotalPoints() > 0) {
                totalPointsReset += lp.getTotalPoints();
                totalCustomers++;

                // Reset
                lp.setTotalPoints(0);
                lp.setPointsAvailable(0);
                lp.setPointsUsed(0);
                loyaltyPointRepository.save(lp);

                // Transaction
                LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createResetTransaction(
                        lp, reason, admin);
                transactionRepository.save(transaction);

                // Update tier
                Customer customer = lp.getCustomer();
                customer.setTier(regularTier);
                customerRepository.save(customer);
            }
        }

        // Audit
        if (adminId != null) {
            auditLogService.logSuccess(
                    adminId,
                    AuditModule.LOYALTY_POINTS,
                    AuditAction.RESET_POINTS,
                    "SYSTEM",
                    "system",
                    null,
                    String.format("Reset %d customers, %d total points. Reason: %s",
                            totalCustomers, totalPointsReset, reason)
            );
        }

        log.info("Reset completed: {} customers, {} points", totalCustomers, totalPointsReset);

        return ResetResultResponse.builder()
                .totalCustomersReset(totalCustomers)
                .totalPointsReset(totalPointsReset)
                .resetDate(LocalDateTime.now())
                .reason(reason)
                .build();
    }

    // ========== TIER MANAGEMENT ==========

    @Override
    @Transactional
    public void updateCustomerTier(UUID customerId) {
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        CustomerTier newTier = getTierByPoints(loyaltyPoint.getTotalPoints());
        Customer customer = loyaltyPoint.getCustomer();

        // So sánh tier hiện tại
        boolean tierChanged = customer.getTier() == null ||
                !customer.getTier().getId().equals(newTier.getId());

        if (tierChanged) {
            String oldTierName = customer.getTier() != null ? customer.getTier().getName() : "NONE";
            customer.setTier(newTier);
            customerRepository.save(customer);
            log.info("Customer {} tier updated: {} -> {}", customerId, oldTierName, newTier.getName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTier getTierByPoints(Integer points) {
        // Lấy tier phù hợp với số điểm (tier có min_point cao nhất mà <= points)
        List<CustomerTier> tiers = customerTierRepository.findTiersByPoints(points);

        if (tiers.isEmpty()) {
            // Fallback: Tìm tier REGULAR
            return customerTierRepository.findByNameAndIsDeletedFalse("REGULAR")
                    .orElseThrow(() -> new RuntimeException(
                            "System error: REGULAR tier not found. Please contact administrator."));
        }

        return tiers.get(0); // Đã sắp xếp DESC, nên lấy cái đầu tiên
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPointsToNextTier(Integer currentPoints) {
        // Lấy tier tiếp theo
        List<CustomerTier> nextTiers = customerTierRepository.findNextTier(currentPoints);

        if (nextTiers.isEmpty()) {
            return null; // Đã đạt tier cao nhất
        }

        CustomerTier nextTier = nextTiers.get(0);
        return nextTier.getMinPoint() - currentPoints;
    }

    // ========== CONFIG MANAGEMENT ==========

    @Override
    @Transactional(readOnly = true)
    public LoyaltyConfigResponse getLoyaltyConfig() {
        // Lấy tier thresholds từ DB
        List<CustomerTier> tiers = customerTierRepository.findAllActiveTiers();
        Map<String, Integer> tierMap = new HashMap<>();

        for (CustomerTier tier : tiers) {
            tierMap.put(tier.getName().toLowerCase() + "MinPoints", tier.getMinPoint());
        }

        return LoyaltyConfigResponse.builder()
                .earnEnabled(getConfigBoolean(EARN_ENABLED, true))
                .earnRate(getConfigInt(EARN_RATE, 100000))
                .minOrderToEarn(getConfigInt(MIN_ORDER_TO_EARN, 0))
                .redeemEnabled(getConfigBoolean(REDEEM_ENABLED, true))
                .redeemRate(getConfigInt(REDEEM_RATE, 1000))
                .maxRedeemPercent(getConfigInt(MAX_REDEEM_PERCENT, 50))
                .minOrderToRedeem(getConfigInt(MIN_ORDER_TO_REDEEM, 0))
                .regularMinPoints(tierMap.getOrDefault("regularMinPoints", 0))
                .loyalMinPoints(tierMap.getOrDefault("loyalMinPoints", 1000))
                .goldMinPoints(tierMap.getOrDefault("goldMinPoints", 5000))
                .platinumMinPoints(tierMap.getOrDefault("platinumMinPoints", 10000))
                .resetEnabled(getConfigBoolean("loyalty.reset.enabled", true))
                .resetPeriodMonths(getConfigInt("loyalty.reset.period_months", 12))
                .nextResetDate(calculateNextResetDate())
                .build();
    }

    @Override
    @Transactional
    public LoyaltyConfigResponse updateLoyaltyConfig(UpdateLoyaltyConfigRequest request, UUID adminId) {
        Map<String, String> updates = new HashMap<>();

        // Update earning/redemption configs
        if (request.getEarnEnabled() != null) {
            updateConfig(EARN_ENABLED, request.getEarnEnabled().toString());
            updates.put(EARN_ENABLED, request.getEarnEnabled().toString());
        }
        if (request.getEarnRate() != null) {
            updateConfig(EARN_RATE, request.getEarnRate().toString());
            updates.put(EARN_RATE, request.getEarnRate().toString());
        }
        if (request.getMinOrderToEarn() != null) {
            updateConfig(MIN_ORDER_TO_EARN, request.getMinOrderToEarn().toString());
            updates.put(MIN_ORDER_TO_EARN, request.getMinOrderToEarn().toString());
        }
        if (request.getRedeemEnabled() != null) {
            updateConfig(REDEEM_ENABLED, request.getRedeemEnabled().toString());
            updates.put(REDEEM_ENABLED, request.getRedeemEnabled().toString());
        }
        if (request.getRedeemRate() != null) {
            updateConfig(REDEEM_RATE, request.getRedeemRate().toString());
            updates.put(REDEEM_RATE, request.getRedeemRate().toString());
        }
        if (request.getMaxRedeemPercent() != null) {
            updateConfig(MAX_REDEEM_PERCENT, request.getMaxRedeemPercent().toString());
            updates.put(MAX_REDEEM_PERCENT, request.getMaxRedeemPercent().toString());
        }
        if (request.getMinOrderToRedeem() != null) {
            updateConfig(MIN_ORDER_TO_REDEEM, request.getMinOrderToRedeem().toString());
            updates.put(MIN_ORDER_TO_REDEEM, request.getMinOrderToRedeem().toString());
        }

        // Update tier thresholds in DB
        updateTierThreshold("LOYAL", request.getLoyalMinPoints());
        updateTierThreshold("GOLD", request.getGoldMinPoints());
        updateTierThreshold("PLATINUM", request.getPlatinumMinPoints());

        // Audit
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.UPDATE,
                "LOYALTY_CONFIG",
                "config",
                null,
                updates.toString()
        );

        return getLoyaltyConfig();
    }

    // ========== HELPER METHODS ==========

    private LoyaltyPoint createLoyaltyPointForCustomer(Customer customer) {
        LoyaltyPoint lp = new LoyaltyPoint();
        lp.setCustomer(customer);
        lp.setTotalPoints(0);
        lp.setPointsAvailable(0);
        lp.setPointsUsed(0);
        lp.setPointRate(new BigDecimal(getConfigInt(REDEEM_RATE, 1000)));
        return loyaltyPointRepository.save(lp);
    }

    private void updateTierThreshold(String tierName, Integer newMinPoint) {
        if (newMinPoint != null) {
            customerTierRepository.findByNameAndIsDeletedFalse(tierName)
                    .ifPresent(tier -> {
                        tier.setMinPoint(newMinPoint);
                        customerTierRepository.save(tier);
                        log.info("Updated tier {} threshold to {}", tierName, newMinPoint);
                    });
        }
    }

    private Boolean getConfigBoolean(String key, Boolean defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private Integer getConfigInt(String key, Integer defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private BigDecimal getConfigBigDecimal(String key, BigDecimal defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> new BigDecimal(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private void updateConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(key);
                    newConfig.setDataType("STRING");
                    return newConfig;
                });
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }

    private LocalDateTime calculateNextResetDate() {
        int currentYear = LocalDateTime.now().getYear();
        LocalDateTime nextReset = LocalDateTime.of(currentYear + 1, 1, 1, 0, 0);

        if (LocalDateTime.now().isAfter(LocalDateTime.of(currentYear, 1, 1, 0, 0))) {
            return nextReset;
        } else {
            return LocalDateTime.of(currentYear, 1, 1, 0, 0);
        }
    }
}