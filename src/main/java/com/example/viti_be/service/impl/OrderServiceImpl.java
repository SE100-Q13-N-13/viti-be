package com.example.viti_be.service.impl;

import com.example.viti_be.dto.mapper.OrderMapper;
import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.request.OrderItemRequest;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderRepository repo;

    @Autowired
    UserRepository userRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    PromotionRepository promotionRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private ProductSerialRepository productSerialRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private AuditLogService auditLogService;

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
            auditLogService.log(actorId, "ORDER", "CREATE",
                    savedOrder.getId().toString(), null, savedOrder.getOrderNumber(), "Order Created");
        }

        return OrderMapper.mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason, UUID actorId) {
        Order order = repo.findById(orderId).
                orElseThrow(() -> new RuntimeException("Can not find order with id: " + orderId.toString()));
        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        switch (newStatus) {
            case CONFIRMED:
                // Logic: Chuyển từ Reserved -> StockOut (Physical giảm)
                confirmStockOutForOrder(order, actorId);
                break;

            case COMPLETED:
                if (oldStatus == OrderStatus.PENDING && order.getOrderType() == OrderType.OFFLINE) {
                    confirmStockOutForOrder(order, actorId);
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
                break;
            default:
                break;
        }

        order.setStatus(newStatus);
        Order savedOrder = repo.save(order);

        if (auditLogService != null) {
            auditLogService.log(actorId, "ORDER", "UPDATE_STATUS",
                    orderId.toString(), oldStatus.toString(), newStatus.toString(), reason);
        }

        return OrderMapper.mapToOrderResponse(savedOrder);
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
        // Hiện tại tạm để 0, logic Promotion sẽ update vào đây
        BigDecimal totalSystemDiscount = BigDecimal.ZERO;

        // ... Apply Promotion logic here ...

        order.setTotalDiscount(totalSystemDiscount);

        // 4. Final Amount = Subtotal - SystemDiscount + Shipping
        BigDecimal finalAmount = subtotal.subtract(totalSystemDiscount);

        // Chặn âm
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

    // TODO: Shipping logic
}
