package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.LoyaltyConfigResponse;
import com.example.viti_be.mapper.OrderMapper;
import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.request.OrderItemRequest;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.LoyaltyPointService;
import com.example.viti_be.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired private OrderRepository repo;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private ProductSerialRepository productSerialRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private LoyaltyPointService loyaltyPointService;
    @Autowired private LoyaltyPointRepository loyaltyPointRepository;
    @Autowired private LoyaltyPointTransactionRepository loyaltyPointTransactionRepository;

    @Override
    public OrderResponse getOrderById(UUID id) {
        Order order = repo.findById(id).orElseThrow(() -> new RuntimeException("Can not find Order by ID: " + id));
        return OrderMapper.mapToOrderResponse(order);
    }
    @Override
    public List<OrderResponse> getAllOrders(){
        return repo.findAll().stream()
                .map(OrderMapper::mapToOrderResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID actorId) {

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndIsDeletedFalse(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
        }

        UUID employeeIdRaw = request.getEmployeeId() != null ? request.getEmployeeId() : actorId;
        User employee = userRepository.findByIdAndIsDeletedFalse(employeeIdRaw)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeIdRaw));

        Order order = buildInitialOrder(request, customer, employee);

        List<OrderItem> orderItems = processOrderItems(request.getItems(), order, actorId);
        order.setItems(orderItems);

        calculateOrderFinancials(order, request);

        Order savedOrder = repo.save(order);

        if (auditLogService != null) {
            auditLogService.log(actorId, AuditModule.ORDER, AuditAction.CREATE,
                    savedOrder.getId().toString(), "order", null, savedOrder.getOrderNumber(), "Order Created");
        }

        return OrderMapper.mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason, UUID actorId) {
        Order order = repo.findById(orderId).
                orElseThrow(() -> new RuntimeException("Can not find order with id: " + orderId));
        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(oldStatus, newStatus);
        Integer pointsEarned = null;

        switch (newStatus) {
            case CONFIRMED:
                // Logic: Chuyển từ Reserved -> StockOut (Physical giảm)
                confirmStockOutForOrder(order, actorId);
                if (order.getLoyaltyPointsUsed() != null && order.getLoyaltyPointsUsed() > 0) {
                    deductLoyaltyPoints(order, actorId);
                }
                pointsEarned = loyaltyPointService.earnPointsFromOrder(order, actorId);
                break;

            case COMPLETED:
                if (oldStatus == OrderStatus.PENDING && order.getOrderType() == OrderType.OFFLINE) {
                    confirmStockOutForOrder(order, actorId);

                    // Deduct và Earn points cho OFFLINE
                    if (order.getLoyaltyPointsUsed() != null && order.getLoyaltyPointsUsed() > 0) {
                        deductLoyaltyPoints(order, actorId);
                    }
                    pointsEarned = loyaltyPointService.earnPointsFromOrder(order, actorId);
                }

                // Set warranty expiration date
                LocalDateTime completionDate = LocalDateTime.now();
                for (OrderItem item : order.getItems()) {
                    Integer months = item.getProductVariant().getProduct().getWarrantyPeriod();
                    if (months != null && months > 0) {
                        item.setWarrantyPeriodSnapshot(completionDate.plusMonths(months));
                    }
                }
                updateCustomerStats(order);
                break;

            case CANCELLED:
                cancelOrderInventory(order, actorId);
                if (order.getLoyaltyPointsUsed() != null && order.getLoyaltyPointsUsed() > 0) {
                    restoreLoyaltyPoints(order, actorId);
                }
                break;
            default:
                break;
        }

        order.setStatus(newStatus);
        Order savedOrder = repo.save(order);

        if (auditLogService != null) {
            auditLogService.log(actorId, AuditModule.ORDER, AuditAction.UPDATE,
                    orderId.toString(), "order", oldStatus.toString(), newStatus.toString(), reason);
        }

        return OrderMapper.mapToOrderResponse(savedOrder, pointsEarned);
    }

    @Override
    public void deleteOrder(UUID id) {
        repo.deleteById(id);
    }
    private List<OrderItem> processOrderItems(List<OrderItemRequest> itemRequests, Order order, UUID actorId) {
        List<OrderItem> finalOrderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            // 1. Validate Product
            ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(itemReq.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + itemReq.getProductVariantId()));

            int quantity = itemReq.getQuantity();
            if (quantity <= 0) throw new BadRequestException("Quantity must be > 0");

            // 2. Gọi Inventory Service để giữ hàng (Reserve Stock)
            // Logic này sẽ trừ available, tăng reserved trong kho
            inventoryService.reserveStock(variant.getId(), quantity, order.getOrderNumber(), actorId);

            // 3. Cấp phát Serial (Quan trọng: Trả về List<ProductSerial> Object)
            // Hàm allocateSerials cần được implement trong InventoryService
            List<ProductSerial> allocatedSerials = inventoryService.allocateSerials(
                    variant.getId(),
                    itemReq.getProductSerialId(), // UUID (có thể null)
                    quantity
            );

            // Validate lại độ dài (đề phòng lỗi logic kho)
            if (allocatedSerials.size() != quantity) {
                throw new BadRequestException("System error: Allocated serials count mismatch.");
            }

            // 4. Xử lý giảm giá thủ công (Manual Discount)
            // Nếu mua 2 cái, giảm 100k -> Mỗi cái giảm 50k
            BigDecimal discountPerItem = BigDecimal.ZERO;
            if (itemReq.getDiscount() != null && itemReq.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                discountPerItem = itemReq.getDiscount().divide(new BigDecimal(quantity), 2, RoundingMode.HALF_DOWN);
            }

            // 5. Tạo OrderItem cho từng Serial (Splitting items)
            for (ProductSerial serial : allocatedSerials) {
                // Lấy snapshot giá
                BigDecimal unitPrice = variant.getSellingPrice();
                // Giá vốn (QUAN TRỌNG ĐỂ TÍNH LÃI): Lấy từ Variant hoặc lô nhập (nếu hệ thống FIFO)
                // Ở đây tạm lấy giá trung bình từ Variant
                BigDecimal costPrice = variant.getPurchasePriceAvg() != null ? variant.getPurchasePriceAvg() : BigDecimal.ZERO;

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .productVariant(variant)
                        .product(variant.getProduct())
                        .productSerial(serial) // SET OBJECT SERIAL
                        .quantity(1) // Item có serial luôn là 1
                        .unitPrice(unitPrice)
                        .costPrice(costPrice)
                        .discount(discountPerItem)
                        // Subtotal = (Unit - Discount) * 1
                        .subtotal(unitPrice.subtract(discountPerItem))
                        // Snapshot bảo hành
                        .warrantyPeriodSnapshot(null)
                        .build();

                finalOrderItems.add(item);

                // Đánh dấu Serial là đã bán (SOLD) hoặc đang xử lý
                inventoryService.markSerialAsSold(serial.getSerialNumber(), order.getId(), actorId);
            }
        }
        return finalOrderItems;
    }

    private Order buildInitialOrder(CreateOrderRequest request, Customer customer, User employee) {
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .employee(employee)
                .orderType(request.getOrderType())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .build();

        // Snapshot hạng thành viên
        if (customer != null && customer.getTier() != null) {
            order.setTierName(customer.getTier().getName());
        }
        return order;
    }

    private void calculateOrderFinancials(Order order, CreateOrderRequest request) {
        // 1. Tổng tiền hàng (đã trừ manual discount ở item)
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);

        // 2. Tính Discount khác (Voucher, Promotion, Rank, Point)
        BigDecimal totalSystemDiscount = BigDecimal.ZERO;

        BigDecimal pointsDiscount = BigDecimal.ZERO;
        BigDecimal promotionDiscount = BigDecimal.ZERO;

        if (request.getLoyaltyPointsToUse() != null && request.getLoyaltyPointsToUse() > 0) {
            // Lấy config hiện tại để lưu snapshot
            LoyaltyConfigResponse loyaltyConfig = loyaltyPointService.getLoyaltyConfig();
            BigDecimal currentPointRate = new BigDecimal(loyaltyConfig.getRedeemRate());

            // Validate và tính toán
            loyaltyPointService.validateRedemption(
                    request.getCustomerId(),
                    request.getLoyaltyPointsToUse(),
                    subtotal.subtract(promotionDiscount)
            );

            pointsDiscount = loyaltyPointService.calculateRedemptionAmount(
                    request.getLoyaltyPointsToUse()
            );

            order.setLoyaltyPointsUsed(request.getLoyaltyPointsToUse());
            order.setPointDiscountAmount(pointsDiscount);

            // Lưu tỷ lệ quy đổi tại thời điểm mua
            order.setPointRateSnapshot(currentPointRate);
        }

        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (order.getCustomer() != null && order.getCustomer().getTier() != null) {
            BigDecimal tierRate = order.getCustomer().getTier().getDiscountRate();
            if (tierRate != null && tierRate.compareTo(BigDecimal.ZERO) > 0) {
                // Tier discount áp dụng trên subtotal (trước promotions)
                tierDiscount = subtotal.multiply(tierRate)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);

                order.setTierDiscountRate(tierRate);
                order.setTierDiscountAmount(tierDiscount);
            }
        }

        // 5. Total Discount = Tier + Promotion + Points
        BigDecimal totalDiscount = tierDiscount.add(promotionDiscount).add(pointsDiscount);
        order.setTotalDiscount(totalDiscount);

        // 6. Final Amount = Subtotal - Total Discount
        BigDecimal finalAmount = subtotal.subtract(totalDiscount);
        order.setFinalAmount(finalAmount.max(BigDecimal.ZERO));
    }

    private void validateStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status from terminal state: " + oldStatus);
        }
        // Thêm các rule chặt chẽ hơn nếu cần
    }

    // Xác nhận xuất kho thật sự (giảm Physical Quantity)
    private void confirmStockOutForOrder(Order order, UUID actorId) {
        for (OrderItem item : order.getItems()) {
            // Với mỗi item, xác nhận xuất kho
            // Vì item đã tách serial, số lượng luôn là 1
            inventoryService.confirmStockOut(
                    item.getProductVariant().getId(),
                    item.getQuantity(),
                    order.getOrderNumber(),
                    actorId
            );
        }
    }

    // Hoàn kho khi hủy đơn
    private void cancelOrderInventory(Order order, UUID actorId) {
        for (OrderItem item : order.getItems()) {
            // 1. Trả lại Reserved Stock
            inventoryService.unreserveStock(
                    item.getProductVariant().getId(),
                    item.getQuantity(),
                    order.getOrderNumber(),
                    actorId
            );

            // 2. Release Serial (chuyển lại thành AVAILABLE)
            if (item.getProductSerial() != null) {
                inventoryService.releaseSerial(item.getProductSerial().getSerialNumber(), actorId);
            }
        }
    }

    private void updateCustomerStats(Order order) {
        if (order.getCustomer() != null) {
            Customer c = order.getCustomer();
            // Cộng dồn doanh số
            BigDecimal currentTotal = c.getTotalPurchase() != null ? c.getTotalPurchase() : BigDecimal.ZERO;
            c.setTotalPurchase(currentTotal.add(order.getFinalAmount()));
            customerRepository.save(c);
        }
    }

    private String generateOrderNumber() {
        // Format: ORD-20250109-12345
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(90000) + 10000;
        return "ORD-" + datePart + "-" + random;
    }

    /**
     * Trừ điểm khi confirm order
     */
    private void deductLoyaltyPoints(Order order, UUID actorId) {
        if (order.getCustomer() == null) {
            log.warn("Cannot deduct points: Order {} has no customer", order.getId());
            return;
        }

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(order.getCustomer().getId())
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        // Double-check: points có đủ không
        if (loyaltyPoint.getPointsAvailable() < order.getLoyaltyPointsUsed()) {
            throw new RuntimeException(String.format(
                    "Insufficient points. Available: %d, Required: %d",
                    loyaltyPoint.getPointsAvailable(), order.getLoyaltyPointsUsed()));
        }

        // Trừ điểm
        loyaltyPoint.setPointsAvailable(loyaltyPoint.getPointsAvailable() - order.getLoyaltyPointsUsed());
        loyaltyPoint.setPointsUsed(loyaltyPoint.getPointsUsed() + order.getLoyaltyPointsUsed());
        loyaltyPoint.setLastUsedAt(LocalDateTime.now());
        loyaltyPointRepository.save(loyaltyPoint);

        // Tạo transaction record
        User employee = userRepository.findById(actorId).orElse(null);
        LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createRedeemTransaction(
                loyaltyPoint,
                order,
                order.getLoyaltyPointsUsed(),
                employee
        );
        loyaltyPointTransactionRepository.save(transaction);

        log.info("Deducted {} points from customer {} for order {}",
                order.getLoyaltyPointsUsed(), order.getCustomer().getId(), order.getId());
    }

    /**
     * Hoàn lại điểm khi cancel order
     */
    private void restoreLoyaltyPoints(Order order, UUID actorId) {
        if (order.getCustomer() == null) {
            log.warn("Cannot restore points: Order {} has no customer", order.getId());
            return;
        }

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(order.getCustomer().getId())
                .orElseThrow(() -> new RuntimeException("Customer loyalty points not found"));

        // Hoàn lại điểm
        loyaltyPoint.setPointsAvailable(loyaltyPoint.getPointsAvailable() + order.getLoyaltyPointsUsed());
        loyaltyPoint.setPointsUsed(Math.max(0, loyaltyPoint.getPointsUsed() - order.getLoyaltyPointsUsed()));
        loyaltyPointRepository.save(loyaltyPoint);

        // Tạo transaction record (manual adjust với lý do refund)
        User user = userRepository.findById(actorId).orElse(null);
        LoyaltyPointTransaction transaction = LoyaltyPointTransaction.createManualAdjustTransaction(
                loyaltyPoint,
                order.getLoyaltyPointsUsed(), // Positive value = adding back
                "Refund from cancelled order #" + order.getId(),
                user
        );
        loyaltyPointTransactionRepository.save(transaction);

        log.info("Restored {} points to customer {} from cancelled order {}",
                order.getLoyaltyPointsUsed(), order.getCustomer().getId(), order.getId());
    }

    // TODO: Shipping logic
}
