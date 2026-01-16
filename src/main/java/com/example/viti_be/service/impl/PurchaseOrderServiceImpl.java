package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.PurchaseOrderItemRequest;
import com.example.viti_be.dto.request.PurchaseOrderRequest;
import com.example.viti_be.dto.request.ReceiveGoodsItemRequest;
import com.example.viti_be.dto.request.ReceiveGoodsRequest;
import com.example.viti_be.dto.response.PartComponentResponse;
import com.example.viti_be.dto.response.PurchaseOrderItemResponse;
import com.example.viti_be.dto.response.PurchaseOrderResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.WarrantyMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.PurchaseOrderService;
import com.example.viti_be.service.SystemConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);
    private static final AuditModule MODULE_NAME = AuditModule.PURCHASE_ORDER;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private ProductSerialRepository productSerialRepository;

    @Autowired
    private PartComponentRepository partComponentRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarrantyMapper warrantyMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request, UUID createdBy) {
        logger.info("Creating new Purchase Order for supplier: {}", request.getSupplierId());

        // 1. Validate Supplier exists
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + request.getSupplierId()));

        // 2. Validate all items exist
        validatePurchaseOrderItems(request.getItems());

        // 3. Generate PO Number
        String poNumber = generatePoNumber();

        // 4. Create Purchase Order with status = DRAFT
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplier(supplier)
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .totalAmount(BigDecimal.ZERO)
                .createdBy(createdBy)
                .build();

        // 5. Create Purchase Order Items
        List<PurchaseOrderItem> items = createPurchaseOrderItems(request.getItems(), purchaseOrder, createdBy);
        items.forEach(purchaseOrder::addItem);

        // 6. Calculate total amount
        purchaseOrder.calculateTotalAmount();

        // 7. Save Purchase Order
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        // 8. Log to audit_logs
        logAudit(createdBy, AuditAction.CREATE, purchaseOrder.getId().toString(), null, purchaseOrder);

        logger.info("Purchase Order created successfully with PO Number: {}", poNumber);
        return mapToResponse(purchaseOrder);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receiveGoods(UUID purchaseOrderId, ReceiveGoodsRequest request, UUID updatedBy) {
        logger.info("Processing goods receipt for PO ID: {}", purchaseOrderId);

        try {
            // 1. Get and validate PO exists and is in DRAFT status
            PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdAndIsDeletedFalse(purchaseOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + purchaseOrderId));

            if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
                throw new BadRequestException("Purchase Order must be in DRAFT status to receive goods. Current status: " + purchaseOrder.getStatus());
            }

            String oldValueJson = toJson(purchaseOrder);

            // 2. Update PO status and delivery date
            purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
            purchaseOrder.setActualDeliveryDate(request.getActualDeliveryDate() != null ? 
                    request.getActualDeliveryDate() : LocalDateTime.now());
            purchaseOrder.setUpdatedBy(updatedBy);

            // 3. Create a map for quick lookup
            Map<UUID, ReceiveGoodsItemRequest> receiveItemMap = request.getItems().stream()
                    .collect(Collectors.toMap(
                            item -> item.getProductVariantId() != null ? item.getProductVariantId() : item.getPartComponentId(),
                            item -> item
                    ));

            // 4. Process each item
            List<DiscrepancyRecord> discrepancies = new ArrayList<>();

            for (PurchaseOrderItem item : purchaseOrder.getItems()) {

                // Xác định ID và SKU/Name để xử lý
                UUID itemId;
                String itemSkuOrName;
                boolean isProduct = item.getProductVariant() != null;

                if (isProduct) {
                    itemId = item.getProductVariant().getId();
                    itemSkuOrName = item.getProductVariant().getSku();
                } else {
                    itemId = item.getPartComponent().getId(); // Lấy ID từ Part
                    itemSkuOrName = item.getPartComponent().getName();
                }

                // Lấy request tương ứng từ Map
                ReceiveGoodsItemRequest receiveItem = receiveItemMap.get(itemId);

                if (receiveItem == null) {
                    continue; // Skip if no receipt info provided for this item
                }

                Integer quantityReceived = receiveItem.getQuantityReceived();
                List<String> serialNumbers = receiveItem.getSerialNumbers();

                // 4.1 Validate Serial Numbers (Chỉ dành cho Product)
                if (isProduct && serialNumbers != null && !serialNumbers.isEmpty()) {
                    if (serialNumbers.size() != quantityReceived) {
                        throw new BadRequestException(
                                String.format("Quantity and Serial count mismatch for SKU %s. Expected: %d, Got: %d",
                                        itemSkuOrName, quantityReceived, serialNumbers.size())
                        );
                    }
                    // Check trùng serial
                    for (String serialNumber : serialNumbers) {
                        if (productSerialRepository.existsBySerialNumber(serialNumber)) {
                            throw new BadRequestException("Serial number already exists: " + serialNumber);
                        }
                    }
                }

                // 4.2 Update quantity_received
                item.setQuantityReceived(quantityReceived);
                item.setUpdatedBy(updatedBy);

                // 4.3 Save Serial Numbers (Chỉ cho Product)
                if (isProduct && serialNumbers != null && !serialNumbers.isEmpty()) {
                    for (String serialNumber : serialNumbers) {
                        ProductSerial productSerial = ProductSerial.builder()
                                .productVariant(item.getProductVariant())
                                .serialNumber(serialNumber)
                                .status(ProductSerialStatus.AVAILABLE)
                                .purchaseOrderId(purchaseOrder.getId())
                                .build();
                        productSerialRepository.save(productSerial);
                    }
                }

                // 4.4 Update Inventory & 4.5 Record Transaction & 4.6 MAC
                if (isProduct) {
                    processProductStockIn(item.getProductVariant(), quantityReceived, purchaseOrder.getPoNumber(), item.getUnitPrice(), updatedBy);
                } else {
                    processPartStockIn(item.getPartComponent(), quantityReceived, purchaseOrder.getPoNumber(), item.getUnitPrice(), updatedBy);
                }

                // 4.7 Check for discrepancy
                if (!quantityReceived.equals(item.getQuantityOrdered())) {
                    discrepancies.add(new DiscrepancyRecord(
                            itemId,
                            item.getQuantityOrdered(),
                            quantityReceived,
                            itemSkuOrName
                    ));
                }
            }

            // 5. Handle discrepancies - Create inventory adjustment if needed
            if (!discrepancies.isEmpty()) {
                createInventoryAdjustment(purchaseOrder.getPoNumber(), discrepancies, updatedBy);
            }

            // 6. Save updated PO
            purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            // 7. Log to audit_logs
            logAudit(updatedBy, AuditAction.RECEIVE_GOODS, purchaseOrder.getId().toString(), oldValueJson, purchaseOrder);

            logger.info("Goods receipt completed for PO Number: {}", purchaseOrder.getPoNumber());
            return mapToResponse(purchaseOrder);
            
        } catch (BadRequestException | ResourceNotFoundException e) {
            logger.error("Error processing goods receipt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing goods receipt", e);
            throw new RuntimeException("Failed to process goods receipt: " + e.getMessage(), e);
        }
    }

    // ==================== Other CRUD Operations ====================

    @Override
    public PurchaseOrderResponse getPurchaseOrderById(UUID id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + id));
        return mapToResponse(purchaseOrder);
    }

    @Override
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByIsDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getPurchaseOrdersBySupplierId(UUID supplierId) {
        return purchaseOrderRepository.findAllBySupplierIdAndIsDeletedFalse(supplierId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PurchaseOrderResponse closePurchaseOrder(UUID purchaseOrderId, UUID updatedBy) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdAndIsDeletedFalse(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + purchaseOrderId));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.RECEIVED) {
            throw new BadRequestException("Only RECEIVED Purchase Orders can be closed. Current status: " + purchaseOrder.getStatus());
        }

        String oldValueJson = toJson(purchaseOrder);
        purchaseOrder.setStatus(PurchaseOrderStatus.CLOSED);
        purchaseOrder.setUpdatedBy(updatedBy);
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        logAudit(updatedBy, AuditAction.CLOSE_PO, purchaseOrder.getId().toString(), oldValueJson, purchaseOrder);

        return mapToResponse(purchaseOrder);
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(UUID purchaseOrderId, UUID deletedBy) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdAndIsDeletedFalse(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + purchaseOrderId));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT Purchase Orders can be deleted. Current status: " + purchaseOrder.getStatus());
        }

        String oldValueJson = toJson(purchaseOrder);
        purchaseOrder.setIsDeleted(true);
        purchaseOrder.setUpdatedBy(deletedBy);
        purchaseOrderRepository.save(purchaseOrder);

        logAudit(deletedBy, AuditAction.DELETE, purchaseOrder.getId().toString() , oldValueJson, null);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Generate PO Number in format: PO-YYYYMMDD-XXXX
     */
    private String generatePoNumber() {
        String datePrefix = "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer maxNumber = purchaseOrderRepository.findMaxPoNumberByPrefix(datePrefix);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return datePrefix + String.format("%04d", nextNumber);
    }

    /**
     * Validate all items (product variants or part components) exist
     */
    private void validatePurchaseOrderItems(List<PurchaseOrderItemRequest> items) {
        for (PurchaseOrderItemRequest item : items) {
            if (item.getProductVariantId() != null) {
                if (!productVariantRepository.existsById(item.getProductVariantId())) {
                    throw new ResourceNotFoundException("Product Variant not found with ID: " + item.getProductVariantId());
                }
            } else if (item.getPartComponentId() != null) {
                // SỬA: Thêm logic check PartComponent
                if (!partComponentRepository.existsById(item.getPartComponentId())) {
                    throw new ResourceNotFoundException("Part Component not found with ID: " + item.getPartComponentId());
                }
            } else {
                throw new BadRequestException("Item must contain either ProductVariantId or PartComponentId");
            }
        }
    }

    /**
     * Create Purchase Order Items from request
     */
    private List<PurchaseOrderItem> createPurchaseOrderItems(
            List<PurchaseOrderItemRequest> itemRequests, 
            PurchaseOrder purchaseOrder, 
            UUID createdBy) {
        
        return itemRequests.stream().map(itemRequest -> {
            PurchaseOrderItem.PurchaseOrderItemBuilder builder = PurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .quantityOrdered(itemRequest.getQuantityOrdered())
                    .unitPrice(itemRequest.getUnitPrice())
                    .createdBy(createdBy);

            if (itemRequest.getProductVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(itemRequest.getProductVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
                builder.productVariant(variant);
            } else if (itemRequest.getPartComponentId() != null) {
                PartComponent part = partComponentRepository.findById(itemRequest.getPartComponentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
                builder.partComponent(part);
            } else {
                throw new BadRequestException("Item must have Product or Part ID");
            }

            PurchaseOrderItem item = builder.build();
            item.calculateSubtotal();
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * Calculate and update Moving Average Cost (MAC)
     * Formula: New_Avg_Price = ((Current_Qty * Current_Avg_Price) + (Received_Qty * Import_Price)) / (Current_Qty + Received_Qty)
     */
    private void updateProductMovingAveragePrice(ProductVariant productVariant, int currentQty, int receivedQty, BigDecimal importPrice) {
        if (receivedQty <= 0) {
            return; // No update needed if no quantity received
        }

        BigDecimal currentAvgPrice = productVariant.getPurchasePriceAvg();
        if (currentAvgPrice == null) {
            currentAvgPrice = BigDecimal.ZERO;
        }

        BigDecimal newAvgPrice;
        int totalQty = currentQty + receivedQty;

        if (totalQty == 0) {
            // Edge case: If total quantity is 0, set average price to import price
            newAvgPrice = importPrice;
        } else if (currentQty == 0) {
            // If no existing stock, average price is simply the import price
            newAvgPrice = importPrice;
        } else {
            // Apply Moving Average formula
            BigDecimal currentValue = currentAvgPrice.multiply(BigDecimal.valueOf(currentQty));
            BigDecimal receivedValue = importPrice.multiply(BigDecimal.valueOf(receivedQty));
            newAvgPrice = currentValue.add(receivedValue)
                    .divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP);
        }

        productVariant.setPurchasePriceAvg(newAvgPrice);
        productVariantRepository.save(productVariant);

        logger.debug("Updated MAC for variant {}: {} -> {}", 
                productVariant.getSku(), currentAvgPrice, newAvgPrice);
    }

    /**
     * Cập nhật giá vốn trung bình cho Linh kiện (PartComponent)
     */
    private void updatePartMovingAveragePrice(PartComponent part, int currentQty, int receivedQty, BigDecimal importPrice) {
        if (receivedQty <= 0) {
            return;
        }

        BigDecimal currentAvgPrice = part.getPurchasePriceAvg();
        if (currentAvgPrice == null) {
            currentAvgPrice = BigDecimal.ZERO;
        }

        BigDecimal newAvgPrice;
        int totalQty = currentQty + receivedQty;

        if (totalQty == 0) {
            newAvgPrice = importPrice;
        } else if (currentQty == 0) {
            newAvgPrice = importPrice;
        } else {
            // Công thức: (Tồn cũ * Giá cũ + Nhập mới * Giá nhập) / Tổng tồn
            BigDecimal currentValue = currentAvgPrice.multiply(BigDecimal.valueOf(currentQty));
            BigDecimal receivedValue = importPrice.multiply(BigDecimal.valueOf(receivedQty));
            newAvgPrice = currentValue.add(receivedValue)
                    .divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP);
        }

        part.setPurchasePriceAvg(newAvgPrice);
        BigDecimal markupPercent = systemConfigService.getPartMarkupPercent();
        BigDecimal multiplier = BigDecimal.ONE.add(markupPercent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal newSellingPrice = newAvgPrice.multiply(multiplier);
        part.setSellingPrice(newSellingPrice);
        partComponentRepository.save(part);

        logger.debug("Updated Part MAC for {}: {} -> {}", part.getName(), currentAvgPrice, newAvgPrice);
    }

    /**
     * Create Inventory Adjustment for discrepancies between ordered and received quantities
     */
    private void createInventoryAdjustment(String poNumber, List<DiscrepancyRecord> discrepancies, UUID createdBy) {
        StringBuilder reason = new StringBuilder("PO #" + poNumber + " discrepancies: ");
        
        for (DiscrepancyRecord d : discrepancies) {
            reason.append(String.format("[%s: Ordered %d, Received %d]; ", 
                    d.sku, d.ordered, d.received));
        }

        InventoryAdjustment adjustment = InventoryAdjustment.builder()
                .referenceCode(UUID.randomUUID())
                .reason(reason.toString().trim())
                .status(InventoryAdjustmentStatus.APPROVED) // Auto-approved for PO discrepancies
                .createdBy(createdBy)
                .build();

        inventoryAdjustmentRepository.save(adjustment);
        logger.info("Created inventory adjustment for PO discrepancies: {}", adjustment.getReferenceCode());
    }

    /**
     * Log audit event
     */
    private void logAudit(UUID actorId, AuditAction action, String resourceId, String oldValue, Object newValue) {
        String newValueJson = newValue != null ? toJson(newValue) : null;
        auditLogService.logSuccess(actorId, MODULE_NAME, action, resourceId, "purchaseOrder", oldValue, newValueJson);
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize object to JSON", e);
            return null;
        }
    }

    /**
     * Map PurchaseOrder entity to PurchaseOrderResponse DTO
     */
    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        Supplier supplier = po.getSupplier();
        PurchaseOrderResponse.SupplierInfo supplierInfo = PurchaseOrderResponse.SupplierInfo.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactName(supplier.getContact_name())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .build();

        List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplier(supplierInfo)
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .actualDeliveryDate(po.getActualDeliveryDate())
                .items(itemResponses)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .build();
    }

    /**
     * Map PurchaseOrderItem entity to PurchaseOrderItemResponse DTO
     */
    private PurchaseOrderItemResponse mapItemToResponse(PurchaseOrderItem item) {
        // 1. Xử lý Product Variant (Có thể null)
        PurchaseOrderItemResponse.ProductVariantInfo pvInfo = null;
        if (item.getProductVariant() != null) {
            ProductVariant pv = item.getProductVariant();
            pvInfo = PurchaseOrderItemResponse.ProductVariantInfo.builder()
                    .id(pv.getId())
                    .sku(pv.getSku())
                    .variantName(pv.getVariantName())
                    .productName(pv.getProduct() != null ? pv.getProduct().getName() : null)
                    .purchasePriceAvg(pv.getPurchasePriceAvg())
                    .sellingPrice(pv.getSellingPrice())
                    .build();
        }

        // 2. Xử lý Part Component
        PartComponentResponse partInfo = null;
        if (item.getPartComponent() != null) {
            PartComponent pc = item.getPartComponent();

            // Map Supplier info nhỏ cho Part
            PartComponentResponse.SupplierInfo supplierInfo = null;
            if (pc.getSupplier() != null) {
                supplierInfo = PartComponentResponse.SupplierInfo.builder()
                        .supplierId(pc.getSupplier().getId())
                        .name(pc.getSupplier().getName())
                        .build();
            }

            partInfo = PartComponentResponse.builder()
                    .id(pc.getId())
                    .name(pc.getName())
                    .partType(pc.getPartType())
                    .unit(pc.getUnit())
                    .supplier(supplierInfo)
                    .purchasePriceAvg(pc.getPurchasePriceAvg())
                    .sellingPrice(pc.getSellingPrice())
                    .minStock(pc.getMinStock())
                    .build();
        }

        // 3. Trả về Response
        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .productVariant(pvInfo)       // Field này null nếu là Part
                .partComponent(partInfo)      // Field này null nếu là Product
                .quantityOrdered(item.getQuantityOrdered())
                .quantityReceived(item.getQuantityReceived())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .warrantyPeriod(item.getWarrantyPeriod())
                .referenceTicketId(item.getReferenceTicketId())
                .build();
    }

    /**
     * Inner class for tracking discrepancies
     */
    private static class DiscrepancyRecord {
        UUID productVariantId;
        Integer ordered;
        Integer received;
        String sku;

        DiscrepancyRecord(UUID productVariantId, Integer ordered, Integer received, String sku) {
            this.productVariantId = productVariantId;
            this.ordered = ordered;
            this.received = received;
            this.sku = sku;
        }
    }

    private void processProductStockIn(ProductVariant variant, int quantity, String poNumber, BigDecimal unitPrice, UUID actorId) {
        Inventory inventory = inventoryService.getOrCreateInventory(variant.getId(), actorId);
        int quantityBefore = inventory.getQuantityPhysical();

        inventory.addStock(quantity);
        inventory.setUpdatedBy(actorId);
        inventoryRepository.save(inventory);

        saveStockTransaction(inventory, quantity, quantityBefore, poNumber, actorId);
        updateProductMovingAveragePrice(variant, quantityBefore, quantity, unitPrice);
    }

    private void processPartStockIn(PartComponent part, int quantity, String poNumber, BigDecimal unitPrice, UUID actorId) {
        // Sử dụng hàm getOrCreatePartInventory bạn đã viết bên InventoryService
        Inventory inventory = inventoryService.getOrCreatePartInventory(part.getId(), actorId);
        int quantityBefore = inventory.getQuantityPhysical();

        inventory.addStock(quantity);
        inventory.setUpdatedBy(actorId);
        inventoryRepository.save(inventory);

        saveStockTransaction(inventory, quantity, quantityBefore, poNumber, actorId);
        updatePartMovingAveragePrice(part, quantityBefore, quantity, unitPrice);
    }

    // Hàm lưu transaction chung
    private void saveStockTransaction(Inventory inventory, int quantity, int qtyBefore, String poNumber, UUID actorId) {
        StockTransaction transaction = StockTransaction.builder()
                .inventory(inventory)
                .type(StockTransactionType.STOCK_IN)
                .quantity(quantity)
                .quantityBefore(qtyBefore)
                .quantityAfter(inventory.getQuantityPhysical())
                .reason("Stock In from PO: " + poNumber)
                .referenceId(poNumber)
                .createdBy(actorId)
                .build();
        stockTransactionRepository.save(transaction);
    }
}
