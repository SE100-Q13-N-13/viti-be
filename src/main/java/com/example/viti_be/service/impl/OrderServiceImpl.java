package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CustomerRequest;
import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.dto.response.LoyaltyConfigResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.mapper.OrderMapper;
import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.request.OrderItemRequest;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.viti_be.mapper.OrderMapper.mapToOrderResponse;

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

    @Autowired private LoyaltyPointTransactionRepository loyaltyPointTransactionRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private LoyaltyPointService loyaltyPointService;
    @Autowired private LoyaltyPointRepository loyaltyPointRepository;
    @Autowired private CustomerService customerService;
    @Autowired private NotificationService notificationService;
    @Autowired private SystemConfigService systemConfigService;

    @Override
    public OrderResponse getOrderById(UUID id) {
        Order order = repo.findById(id).orElseThrow(() -> new RuntimeException("Can not find Order by ID: " + id));
        return mapToOrderResponse(order);
    }
    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable){
        Page<Order> orderPage = repo.findAll(pageable);
        return orderPage.map(OrderMapper::mapToOrderResponse);
    }
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID actorId) {

        // ========== BƯỚC 1: Xác định employeeId và customerId dựa trên orderType ==========
        UUID employeeId;
        UUID customerId;
        Customer customer = null;
        User employee = null;

        switch (request.getOrderType()) {
            case OFFLINE:
                // Nhân viên bán hàng tại quầy
                if (actorId == null) {
                    throw new BadRequestException("Authentication required for OFFLINE orders");
                }

                // Actor = Employee
                employeeId = actorId;
                employee = userRepository.findByIdAndIsDeletedFalse(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

                // CustomerId từ request (có thể null cho guest tại quầy)
                customerId = request.getCustomerId();
                if (customerId != null) {
                    customer = customerRepository.findByIdAndIsDeletedFalse(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                }
                else {
                    customer = createGuestCustomer(request);
                    customerId = customer.getId();
                }
                break;

            case ONLINE_COD:
            case ONLINE_TRANSFER:
                // Không có nhân viên
                employeeId = null;

                if (actorId != null) {
                    // Khách hàng đã đăng nhập (actorId = userId)
                    customer = customerRepository.findByUserIdAndIsDeletedFalse(actorId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Customer profile not found for user: " + actorId));
                    customerId = customer.getId();
                } else {
                    // Guest checkout (actorId = null)
                    customerId = null;
                    customer = createGuestCustomer(request);
                }
                break;

            default:
                throw new BadRequestException("Invalid order type: " + request.getOrderType());
        }

        // ========== BƯỚC 2: Validate ==========
        validateOrderRequest(request, customerId);

        // ========== BƯỚC 3: Build Order Entity ==========
        Order order = buildOrderEntity(request, customer, employee);

        // ========== BƯỚC 4: Process Order Items (CORE LOGIC) ==========
        // ActorId để ghi log: ưu tiên employeeId, không có thì customerId, không có thì null
        UUID processingActorId = (employeeId != null) ? employeeId : customerId;
        List<OrderItem> orderItems = processOrderItems(request.getItems(), order, processingActorId);
        order.setItems(orderItems);

        // ========== BƯỚC 5: Calculate Order Total ==========
        calculateOrderTotal(order, request);

        // ========== BƯỚC 6: Save Order ==========
        order = repo.save(order);

        // ========== BƯỚC 6.5: Update Serial with OrderId ==========
        for (OrderItem item : order.getItems()) {
            if (item.getProductSerial() != null) {
                inventoryService.markSerialAsSold(
                        item.getProductSerial().getSerialNumber(),
                        order.getId(), // ← Bây giờ có ID rồi
                        processingActorId
                );
            }
        }

        // ========== BƯỚC 7: Send Notification (nếu ONLINE) ==========
        if (order.getOrderType() == OrderType.ONLINE_COD ||
                order.getOrderType() == OrderType.ONLINE_TRANSFER) {
            notificationService.notifyNewOrder(order.getId(), order.getOrderNumber());
        }

        // ========== BƯỚC 8: Return Response ==========
        return mapToOrderResponse(order);
    }

    private Order buildOrderEntity(CreateOrderRequest request, Customer customer, User employee) {
        String orderNumber = generateOrderNumber();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setEmployee(employee);
        order.setOrderType(request.getOrderType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);

        // Shipping address (cho ONLINE orders)
        if (request.getOrderType() == OrderType.ONLINE_COD ||
                request.getOrderType() == OrderType.ONLINE_TRANSFER) {
            if (request.getCustomerId() == null){
                order.setShippingAddress(request.getShippingAddress());
            }
            else {
                Address address = addressRepository.findById(request.getAddressId())
                        .orElseThrow(() -> new BadRequestException("Shipping address id is required for ONLINE orders"));
            }
        }

        // Snapshot tier info (nếu có customer)
        if (customer != null && customer.getTier() != null) {
            order.setTierName(customer.getTier().getName());
            // TODO: Set tierDiscountRate if needed
        }

        return order;
    }

    private void validateOrderRequest(CreateOrderRequest request, UUID resolvedCustomerId) {
        // 1. Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must have at least one item");
        }

        // 2. Validate OFFLINE order
        if (request.getOrderType() == OrderType.OFFLINE) {
            // Nếu không có customerId, bắt buộc có guest info
            if (resolvedCustomerId == null) {
                if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
                    throw new BadRequestException("Guest name is required for offline guest order");
                }
                if (request.getGuestPhone() == null || request.getGuestPhone().trim().isEmpty()) {
                    throw new BadRequestException("Guest phone is required for offline guest order");
                }
            }
        }

        // 3. Validate ONLINE order
        if (request.getOrderType() == OrderType.ONLINE_COD ||
                request.getOrderType() == OrderType.ONLINE_TRANSFER) {

            // Guest checkout: Bắt buộc có thông tin guest
            if (resolvedCustomerId == null) {
                if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
                    throw new BadRequestException("Guest name is required for guest checkout");
                }
                if (request.getGuestPhone() == null || request.getGuestPhone().trim().isEmpty()) {
                    throw new BadRequestException("Guest phone is required for guest checkout");
                }
                if (request.getGuestEmail() == null || request.getGuestEmail().trim().isEmpty()) {
                    throw new BadRequestException("Guest email is required for online guest checkout");
                }
                if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
                    throw new BadRequestException("Shipping address is required for Guest ONLINE orders");
                }
            }
            else {
                if (request.getAddressId() == null){
                    throw new BadRequestException("Shipping address id is required for ONLINE orders");
                }
            }

        }

        // 4. Validate loyalty points (chỉ cho registered customer)
        if (request.getLoyaltyPointsToUse() != null && request.getLoyaltyPointsToUse() > 0) {
            if (resolvedCustomerId == null) {
                throw new BadRequestException("Guest checkout cannot use loyalty points");
            }
        }
    }

    // ========== UPDATE calculateOrderTotal signature ==========
    private void calculateOrderTotal(Order order, CreateOrderRequest request) {
        // 1. Subtotal từ items (đã bao gồm manual discount)
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);

        BigDecimal totalDiscount = BigDecimal.ZERO;

        // 2. Tier Discount (áp dụng đầu tiên)
        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (order.getCustomer() != null && order.getCustomer().getTier() != null) {
            BigDecimal tierRate = order.getCustomer().getTier().getDiscountRate();
            if (tierRate != null && tierRate.compareTo(BigDecimal.ZERO) > 0) {
                tierDiscount = subtotal.multiply(tierRate)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
                order.setTierDiscountRate(tierRate);
                order.setTierDiscountAmount(tierDiscount);
                totalDiscount = totalDiscount.add(tierDiscount);
            }
        }

        // 3. Promotion Discount
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        if (request.getPromotionIds() != null && !request.getPromotionIds().isEmpty()) {
            promotionDiscount = applyPromotions(order, request.getPromotionIds());
            totalDiscount = totalDiscount.add(promotionDiscount);
        }

        // 4. Loyalty Points Discount (áp dụng sau cùng)
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (request.getLoyaltyPointsToUse() != null && request.getLoyaltyPointsToUse() > 0) {
            // Validate customer có đủ điểm không
            if (order.getCustomer() != null) {
                LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(order.getCustomer().getId())
                        .orElse(null);

                if (loyaltyPoint == null || loyaltyPoint.getPointsAvailable() < request.getLoyaltyPointsToUse()) {
                    throw new BadRequestException(
                            String.format("Insufficient loyalty points. Available: %d, Requested: %d",
                                    loyaltyPoint != null ? loyaltyPoint.getPointsAvailable() : 0,
                                    request.getLoyaltyPointsToUse())
                    );
                }

                // Get config và tính discount
                LoyaltyConfigResponse config = loyaltyPointService.getLoyaltyConfig();
                BigDecimal pointRate = new BigDecimal(config.getRedeemRate());

                // Calculate amount after tier and promotion discounts
                BigDecimal amountAfterDiscounts = subtotal.subtract(tierDiscount).subtract(promotionDiscount);

                // Validate redemption amount
                loyaltyPointService.validateRedemption(
                        order.getCustomer().getId(),
                        request.getLoyaltyPointsToUse(),
                        amountAfterDiscounts
                );

                pointsDiscount = loyaltyPointService.calculateRedemptionAmount(
                        request.getLoyaltyPointsToUse()
                );

                // Không cho phép discount vượt quá số tiền còn lại
                pointsDiscount = pointsDiscount.min(amountAfterDiscounts);

                order.setLoyaltyPointsUsed(request.getLoyaltyPointsToUse());
                order.setPointDiscountAmount(pointsDiscount);
                order.setPointRateSnapshot(pointRate);
                totalDiscount = totalDiscount.add(pointsDiscount);
            }
        }

        // 5. Final calculation
        order.setTotalDiscount(totalDiscount);
        order.setFinalAmount(subtotal.subtract(totalDiscount).max(BigDecimal.ZERO));
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

        return mapToOrderResponse(savedOrder, pointsEarned);
    }

    @Override
    public void deleteOrder(UUID id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public PageResponse<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        // 1. Kiểm tra xem User này có phải là Customer không
        Optional<Customer> customerOpt = customerRepository.findByUser_Id(userId);

        Page<Order> orderPage;

        if (customerOpt.isPresent()) {
            // ==> Là Customer: Lấy danh sách đơn hàng họ đã mua
            orderPage = repo.findByCustomer_Id(customerOpt.get().getId(), pageable);
        } else {
            // ==> Không tìm thấy Customer Profile -> Coi là Employee (User): Lấy đơn hàng họ phụ trách
            // Lưu ý: userId ở đây chính là id của bảng Users
            orderPage = repo.findByEmployee_Id(userId, pageable);
        }

        // 2. Map Entity sang Response DTO
        return PageResponse.from(orderPage, OrderMapper::mapToOrderResponse);
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

                // Get warranty period from system config
                Integer warrantyMonths = systemConfigService.getWarrantyPeriodMonths();
                LocalDateTime warrantyExpireDate = LocalDateTime.now().plusMonths(warrantyMonths);
                
                // Update serial warranty expire date when sold
                serial.setWarrantyExpireDate(warrantyExpireDate);
                productSerialRepository.save(serial);

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
                        .warrantyPeriodSnapshot(warrantyExpireDate)
                        .build();

                finalOrderItems.add(item);
            }
        }
        return finalOrderItems;
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

    private Customer createGuestCustomer(CreateOrderRequest request) {
        // Validate guest info
        if (request.getGuestPhone() == null || request.getGuestPhone().isBlank()) {
            throw new BadRequestException("Guest phone is required");
        }

        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new BadRequestException("Guest name is required");
        }

        // Kiểm tra phone đã tồn tại chưa
        Optional<Customer> existingCustomer = customerRepository.findByPhoneAndIsDeletedFalse(request.getGuestPhone());
        if (existingCustomer.isPresent()) {
            log.info("Found existing customer with phone: {}", request.getGuestPhone());
            // Update email nếu có thông tin mới
            Customer customer = existingCustomer.get();
            if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()) {
                customer.setEmail(request.getGuestEmail());
                customerRepository.save(customer);
            }
            return customer;
        }

        // Tạo CustomerRequest từ guest info
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFullName(request.getGuestName());
        customerRequest.setPhone(request.getGuestPhone());
        customerRequest.setEmail(request.getGuestEmail());
        customerRequest.setTierId(null); // Guest không có tier

        // Gọi CustomerService để tạo
        CustomerResponse customerResponse = customerService.createCustomer(customerRequest);

        // Lấy lại Customer entity
        return customerRepository.findById(customerResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to create guest customer"));
    }

    private BigDecimal applyPromotions(Order order, List<UUID> promotionIds) {
        if (promotionIds == null || promotionIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPromotionDiscount = BigDecimal.ZERO;
        List<OrderPromotion> appliedPromotions = new ArrayList<>();

        for (UUID promotionId : promotionIds) {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + promotionId));

            // Validate promotion is active
            if (!isPromotionValid(promotion)) {
                log.warn("Promotion {} is not valid, skipping", promotionId);
                continue;
            }

            // Calculate discount for this promotion
            BigDecimal discountAmount = calculatePromotionDiscount(order, promotion);

            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Create OrderPromotion record
                OrderPromotion orderPromotion = OrderPromotion.builder()
                        .order(order)
                        .promotion(promotion)
                        .discountAmount(discountAmount)
                        .build();

                appliedPromotions.add(orderPromotion);
                totalPromotionDiscount = totalPromotionDiscount.add(discountAmount);
            }
        }

        order.setAppliedPromotions(appliedPromotions);
        return totalPromotionDiscount;
    }

    /**
     * Validate if promotion is currently active and applicable
     */
    private boolean isPromotionValid(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();

        // Check dates
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return false;
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return false;
        }

        // Check status
        if (promotion.getStatus() == null || !promotion.getStatus().name().equals("ACTIVE")) {
            return false;
        }

        // Check usage limit
        if (promotion.getUsageLimit() != null &&
                promotion.getUsageCount() != null &&
                promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return false;
        }

        return true;
    }

    /**
     * Calculate discount amount for a specific promotion
     */
    private BigDecimal calculatePromotionDiscount(Order order, Promotion promotion) {
        BigDecimal subtotal = order.getSubtotal();

        // Check minimum order value
        if (promotion.getMinOrderValue() != null &&
                subtotal.compareTo(promotion.getMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        // Calculate based on promotion type
        switch (promotion.getType()) {
            case PERCENTAGE:
                discount = subtotal.multiply(promotion.getValue())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);

                // Apply max discount cap if exists
                if (promotion.getMaxDiscountAmount() != null) {
                    discount = discount.min(promotion.getMaxDiscountAmount());
                }
                break;

            case FIXED_AMOUNT:
                discount = promotion.getValue();
                break;

            default:
                log.warn("Unknown promotion type: {}", promotion.getType());
        }

        return discount;
    }

    // TODO: Shipping logic
}
