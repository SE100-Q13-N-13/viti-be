package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.InventoryResponse;
import com.example.viti_be.dto.response.ProductSerialResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.ProductSerialStatus;
import com.example.viti_be.model.model_enum.StockTransactionType;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductSerialRepository productSerialRepository;

    @Autowired
    private PartComponentRepository partComponentRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Override
    @Transactional
    public Inventory getOrCreateInventory(UUID productVariantId, UUID createdBy) {
        return inventoryRepository.findByProductVariantId(productVariantId)
                .orElseGet(() -> createNewInventory(productVariantId, createdBy));
    }

    @Transactional
    @Override
    public Inventory getOrCreatePartInventory(UUID partId, UUID createdBy) {
        return inventoryRepository.findByPartComponentId(partId)
                .orElseGet(() -> {
                    PartComponent part = partComponentRepository.findByIdAndIsDeletedFalse(partId)
                            .orElseThrow(() -> new ResourceNotFoundException("Part Component not found with ID: " + partId));

                    Inventory inventory = Inventory.builder()
                            .partComponentId(part.getId())
                            .productVariant(null)
                            .quantityPhysical(0)
                            .quantityReserved(0)
                            .quantityAvailable(0)
                            .minThreshold(part.getMinStock() != null ? part.getMinStock() : 0)
                            .createdBy(createdBy)
                            .build();

                    return inventoryRepository.save(inventory);
                });
    }

    private Inventory createNewInventory(UUID productVariantId, UUID createdBy) {
        ProductVariant productVariant = productVariantRepository.findByIdAndIsDeletedFalse(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with ID: " + productVariantId));

        Inventory inventory = Inventory.builder()
                .productVariant(productVariant)
                .quantityPhysical(0)
                .quantityReserved(0)
                .quantityAvailable(0)
                .minThreshold(productVariant.getProduct() != null ? 
                        productVariant.getProduct().getMinStockThreshold() : 10)
                .createdBy(createdBy)
                .build();

        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory addStock(UUID productVariantId, int quantity, UUID createdBy) {
        Inventory inventory = getOrCreateInventory(productVariantId, createdBy);
        inventory.addStock(quantity);
        inventory.setUpdatedBy(createdBy);
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory reduceStock(UUID productVariantId, int quantity, String reason, UUID createdBy) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }

        // Get or create inventory
        Inventory inventory = getOrCreateInventory(productVariantId, createdBy);

        // Check available stock
        if (inventory.getQuantityAvailable() < quantity) {
            throw new BadRequestException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            inventory.getQuantityAvailable(), quantity)
            );
        }

        // Reduce stock
        int before = inventory.getQuantityPhysical();
        inventory.setQuantityPhysical(inventory.getQuantityPhysical() - quantity);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
        int after = inventory.getQuantityPhysical();

        // Create stock transaction
        StockTransaction transaction = StockTransaction.builder()
                .inventory(inventory)
                .type(StockTransactionType.STOCK_OUT)
                .quantity(-quantity)  // Negative for decrease
                .quantityBefore(before)
                .quantityAfter(after)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        stockTransactionRepository.save(transaction);

        return inventoryRepository.save(inventory);
    }

    @Override
    public InventoryResponse getInventoryByProductVariantId(UUID productVariantId) {
        Inventory inventory = inventoryRepository.findByProductVariantId(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product variant: " + productVariantId));
        return mapToResponse(inventory);
    }

    @Override
    public PageResponse<InventoryResponse> getAllInventory(Pageable pageable) {
        Page<Inventory> inventoryPage = inventoryRepository.findAllByIsDeletedFalse(pageable);
        return PageResponse.from(inventoryPage, this::mapToResponse);
    }

    @Override
    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSerialResponse> getSerialsByProductVariantId(UUID productVariantId, ProductSerialStatus status) {
        List<ProductSerial> serials;
        if (status != null) {
            serials = productSerialRepository.findByProductVariantIdAndStatus(productVariantId, status);
        } else {
            serials = productSerialRepository.findByProductVariantId(productVariantId);
        }
        return serials.stream().map(this::mapSerialToResponse).collect(Collectors.toList());
    }

    @Override
    public ProductSerialResponse getSerialByNumber(String serialNumber) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        return mapSerialToResponse(serial);
    }



    @Override
    @Transactional
    public ProductSerial updateSerialStatus(String serialNumber, ProductSerialStatus newStatus, UUID updatedBy) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        
        serial.setStatus(newStatus);
        return productSerialRepository.save(serial);
    }

    @Override
    public List<ProductSerialResponse> getSerialsByStatus(ProductSerialStatus status) {
        return productSerialRepository.findByStatus(status).stream()
                .map(this::mapSerialToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Methods for Order Module ====================

    @Override
    public boolean isSerialAvailableForSale(String serialNumber) {
        return productSerialRepository.findBySerialNumber(serialNumber)
                .map(serial -> serial.getStatus() == ProductSerialStatus.AVAILABLE)
                .orElse(false);
    }

    @Override
    @Transactional
    public ProductSerial markSerialAsSold(String serialNumber, UUID orderId, UUID updatedBy) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        
        // Validate: Serial must be AVAILABLE to be sold
        if (serial.getStatus() != ProductSerialStatus.AVAILABLE) {
            throw new BadRequestException(
                String.format("Serial %s cannot be sold. Current status: %s", serialNumber, serial.getStatus())
            );
        }
        
        serial.setStatus(ProductSerialStatus.SOLD);
        serial.setOrderId(orderId);
        serial.setSoldDate(java.time.LocalDateTime.now());
        
        return productSerialRepository.save(serial);
    }

    @Override
    @Transactional
    public ProductSerial releaseSerial(String serialNumber, UUID updatedBy) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        
        // Set back to AVAILABLE
        serial.setStatus(ProductSerialStatus.AVAILABLE);
        serial.setOrderId(null);
        serial.setSoldDate(null);
        
        return productSerialRepository.save(serial);
    }

    @Override
    @Transactional
    public void markSerialsAsSold(List<String> serialNumbers, UUID orderId, UUID updatedBy) {
        for (String serialNumber : serialNumbers) {
            markSerialAsSold(serialNumber, orderId, updatedBy);
        }
    }

    @Override
    @Transactional
    public void releaseSerials(List<String> serialNumbers, UUID updatedBy) {
        for (String serialNumber : serialNumbers) {
            releaseSerial(serialNumber, updatedBy);
        }
    }

    @Override
    @Transactional
    public ProductSerial markSerialAsWarranty(String serialNumber, java.time.LocalDateTime warrantyExpireDate, UUID updatedBy) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        
        serial.setStatus(ProductSerialStatus.WARRANTY);
        serial.setWarrantyExpireDate(warrantyExpireDate);
        
        return productSerialRepository.save(serial);
    }

    @Override
    @Transactional
    public ProductSerial markSerialAsDefective(String serialNumber, UUID updatedBy) {
        ProductSerial serial = productSerialRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialNumber));
        
        serial.setStatus(ProductSerialStatus.DEFECTIVE);
        
        return productSerialRepository.save(serial);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveStock(UUID productVariantId, int quantity, String orderRef, UUID actorId) {
        // Lock row trong DB (nếu cần) hoặc dùng logic check thông thường
        Inventory inventory = getOrCreateInventory(productVariantId, actorId);

        if (inventory.getQuantityAvailable() < quantity) {
            throw new BadRequestException("No stock available for the required product");
        }

        // Logic chuyển dịch trạng thái kho
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);

        // TODO: Log inventory history transaction (StockTransaction)
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reservePartStock(UUID partId, int quantity, String ref, UUID actorId) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng giữ hàng phải lớn hơn 0");
        }

        // 1. Lấy thông tin kho
        Inventory inventory = getOrCreatePartInventory(partId, actorId);

        // 2. Kiểm tra tồn kho khả dụng (Available)
        if (inventory.getQuantityAvailable() < quantity) {
            // Lấy tên linh kiện để báo lỗi rõ ràng
            String partName = partComponentRepository.findById(partId)
                    .map(PartComponent::getName)
                    .orElse("Unknown Part");

            throw new BadRequestException(String.format(
                    "Không đủ linh kiện '%s' trong kho. Yêu cầu: %d, Còn: %d",
                    partName, quantity, inventory.getQuantityAvailable()
            ));
        }

        // 3. Thực hiện giữ hàng (Available giảm, Reserved tăng)
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);

        // TODO: Ghi log StockTransaction (Type: RESERVE) nếu cần
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unreserveStock(UUID productVariantId, int quantity, String orderRef, UUID actorId) {
        Inventory inventory = getOrCreateInventory(productVariantId, actorId);

        // Trả lại hàng vào Available
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);

        // Giảm Reserved
        int newReserved = inventory.getQuantityReserved() - quantity;
        if (newReserved < 0) {
            // Trường hợp này hiếm khi xảy ra nếu logic đúng, nhưng cần handle safety
            // Có thể log warning
            newReserved = 0;
        }
        inventory.setQuantityReserved(newReserved);
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unreservePartStock(UUID partId, int quantity, String ref, UUID actorId) {
        if (quantity <= 0) {
            return; // Không làm gì nếu số lượng <= 0
        }

        // 1. Lấy kho linh kiện
        Inventory inventory = getOrCreatePartInventory(partId, actorId);

        // 2. Logic: Chuyển từ Reserved (Đã giữ) -> Available (Khả dụng)

        // Tăng lượng khả dụng
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);

        // Giảm lượng đã giữ
        int currentReserved = inventory.getQuantityReserved();
        int newReserved = currentReserved - quantity;

        // Safety check: Tránh số âm nếu dữ liệu kho bị lệch
        if (newReserved < 0) {
            // Log warning để dev biết có sự bất thường
             log.warn("Part Inventory Inconsistency: ID={} Ref={} Reserved={} Unreserve={}",
                      partId, ref, currentReserved, quantity);
            newReserved = 0;
        }

        inventory.setQuantityReserved(newReserved);
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);

        // TODO: Ghi log StockTransaction (Type: UNRESERVE / CANCEL)
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmStockOut(UUID productVariantId, int quantity, String orderRef, UUID actorId) {
        Inventory inventory = getOrCreateInventory(productVariantId, actorId);

        // Khi confirm, hàng đã nằm trong Reserved rồi.
        if (inventory.getQuantityReserved() < quantity) {
            throw new BadRequestException("Lỗi dữ liệu kho: Số lượng Reserved ít hơn số lượng cần xuất.");
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventory.setQuantityPhysical(inventory.getQuantityPhysical() - quantity); // Giảm tổng kho thực tế
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirmPartStockOut(UUID partId, int quantity, String ref, UUID actorId) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng xuất kho phải lớn hơn 0");
        }

        // 1. Lấy thông tin kho
        Inventory inventory = getOrCreatePartInventory(partId, actorId);

        // 2. Kiểm tra lượng hàng đã giữ (Reserved)
        // Lưu ý: Nghiệp vụ chuẩn là Phải Reserve trước rồi mới Confirm Out.
        // Nếu quantityReserved < quantity nghĩa là chưa Reserve hoặc logic sai.
        if (inventory.getQuantityReserved() < quantity) {
            // Fallback: Nếu chưa reserve (trường hợp sửa nhanh), kiểm tra Available và trừ thẳng
            if (inventory.getQuantityAvailable() >= quantity) {
                // Trừ thẳng vào Available và Physical (Bỏ qua bước Reserve)
                inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
                inventory.setQuantityPhysical(inventory.getQuantityPhysical() - quantity);
                inventory.setUpdatedBy(actorId);
                inventoryRepository.save(inventory);
                return;
            } else {
                throw new BadRequestException("Lỗi kho linh kiện: Số lượng cần xuất vượt quá số lượng đã giữ (Reserved) và khả dụng (Available).");
            }
        }

        // 3. Thực hiện xuất kho thật sự (Reserved giảm, Physical giảm)
        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventory.setQuantityPhysical(inventory.getQuantityPhysical() - quantity);
        inventory.setUpdatedBy(actorId);

        inventoryRepository.save(inventory);

        // TODO: Ghi log StockTransaction (Type: OUT) nếu cần
    }

    @Override
    public List<ProductSerial> allocateSerials(UUID productVariantId, UUID requestSerialId, int quantity) {
        List<ProductSerial> result = new ArrayList<>();

        if (requestSerialId != null) {
            // CASE 1: Mua chỉ định (Offline scan mã vạch hoặc chọn đích danh trên web)
            ProductSerial serial = productSerialRepository.findById(requestSerialId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serial ID not found: " + requestSerialId));

            // Validate logic
            if (!serial.getProductVariant().getId().equals(productVariantId)) {
                throw new BadRequestException("Serial không khớp với sản phẩm trong đơn hàng.");
            }
            if (serial.getStatus() != ProductSerialStatus.AVAILABLE) {
                throw new BadRequestException("Serial " + serial.getSerialNumber() + " không khả dụng (Trạng thái: " + serial.getStatus() + ")");
            }

            // Với case chỉ định, số lượng thường là 1 (đã được tách dòng ở OrderService)
            result.add(serial);

        } else {
            // CASE 2: Mua tự động (Online hoặc không cần chọn serial) -> Lấy theo FIFO
            List<ProductSerial> serials = productSerialRepository.findAvailableByVariantId(
                    productVariantId,
                    PageRequest.of(0, quantity) // Lấy đúng số lượng cần thiết
            );

            if (serials.size() < quantity) {
                throw new BadRequestException(String.format(
                        "Không đủ Serial khả dụng trong kho. Cần: %d, Tìm thấy: %d", quantity, serials.size()
                ));
            }
            result.addAll(serials);
        }

        return result;
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        ProductVariant pv = inventory.getProductVariant();
        
        InventoryResponse.ProductVariantInfo pvInfo = null;
        if (pv != null) {
            pvInfo = InventoryResponse.ProductVariantInfo.builder()
                    .id(pv.getId())
                    .sku(pv.getSku())
                    .variantName(pv.getVariantName())
                    .productName(pv.getProduct() != null ? pv.getProduct().getName() : null)
                    .build();
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productVariant(pvInfo)
                .quantityPhysical(inventory.getQuantityPhysical())
                .quantityReserved(inventory.getQuantityReserved())
                .quantityAvailable(inventory.getQuantityAvailable())
                .minThreshold(inventory.getMinThreshold())
                .lastCountedAt(inventory.getLastCountedAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private ProductSerialResponse mapSerialToResponse(ProductSerial serial) {
        ProductVariant pv = serial.getProductVariant();
        
        ProductSerialResponse.ProductVariantInfo pvInfo = null;
        if (pv != null) {
            pvInfo = ProductSerialResponse.ProductVariantInfo.builder()
                    .id(pv.getId())
                    .sku(pv.getSku())
                    .variantName(pv.getVariantName())
                    .productName(pv.getProduct() != null ? pv.getProduct().getName() : null)
                    .build();
        }
        
        return ProductSerialResponse.builder()
                .id(serial.getId())
                .serialNumber(serial.getSerialNumber())
                .status(serial.getStatus())
                .productVariant(pvInfo)
                .purchaseOrderId(serial.getPurchaseOrderId())
                .orderId(serial.getOrderId())
                .soldDate(serial.getSoldDate())
                .warrantyExpireDate(serial.getWarrantyExpireDate())
                .build();
    }

}
