package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.InventoryResponse;
import com.example.viti_be.dto.response.ProductSerialResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.Inventory;
import com.example.viti_be.model.ProductSerial;
import com.example.viti_be.model.ProductVariant;
import com.example.viti_be.model.model_enum.ProductSerialStatus;
import com.example.viti_be.repository.InventoryRepository;
import com.example.viti_be.repository.ProductSerialRepository;
import com.example.viti_be.repository.ProductVariantRepository;
import com.example.viti_be.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductSerialRepository productSerialRepository;

    @Override
    @Transactional
    public Inventory getOrCreateInventory(UUID productVariantId, UUID createdBy) {
        return inventoryRepository.findByProductVariantId(productVariantId)
                .orElseGet(() -> createNewInventory(productVariantId, createdBy));
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
    public InventoryResponse getInventoryByProductVariantId(UUID productVariantId) {
        Inventory inventory = inventoryRepository.findByProductVariantId(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product variant: " + productVariantId));
        return mapToResponse(inventory);
    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAllByIsDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
